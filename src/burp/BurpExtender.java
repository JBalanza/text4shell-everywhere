package burp;

import burp.*;
import com.sun.xml.internal.messaging.saaj.util.Base64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class BurpExtender implements IBurpExtender {
    private static final String name = "Collaborator Everywhere";
    private static final String version = "0.1";


    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks) {
        new Utilities(callbacks);
        callbacks.setExtensionName(name);

        Correlator collab = new Correlator();

        Monitor collabMonitor = new Monitor(collab);
        new Thread(collabMonitor).start();
        callbacks.registerExtensionStateListener(collabMonitor);

        callbacks.registerHttpListener(new Injector(collab));

        Utilities.out("Loaded " + name + " v" + version);
    }
}

class Monitor implements Runnable, IExtensionStateListener {
    private Correlator collab;
    private boolean stop = false;

    Monitor(Correlator collab) {
        this.collab = collab;
    }

    public void extensionUnloaded() {
        Utilities.out("Extension unloading - triggering abort");
        stop = true;
        Thread.currentThread().interrupt();
    }

    public void run() {
        try {
            while (!stop) {
                Thread.sleep(10000);
                Utilities.out("Polling!");

                collab.poll().forEach(e -> processInteraction(e));
            }
        }
        catch (InterruptedException e) {
            Utilities.out("Interrupted");
        }

        Utilities.out("Shutting down collaborator monitor thread");
    }

    private void processInteraction(IBurpCollaboratorInteraction interaction) {
        String id = interaction.getProperty("interaction_id");
        Utilities.out("Got an interaction:"+interaction.getProperties());
        IHttpRequestResponse req = collab.getRequest(id);
        String type = collab.getType(id);

        String detail = interaction.getProperty("request");
        if (detail == null) {
            detail = interaction.getProperty("conversation");
        }

        if (detail == null) {
            detail = interaction.getProperty("raw_query");
        }

        detail = "<pre>"+Base64.base64Decode(detail).replace("<", "&lt;")+"</pre><br/><br/>";


        Utilities.callbacks.addScanIssue(
                new CustomScanIssue(req.getHttpService(), req.getUrl(), new IHttpRequestResponse[]{req}, "Collaborator Pingback: "+type, detail+interaction.getProperties().toString(), "Information", "Certain", "Panic"));

    }

}

class Correlator {

    private IBurpCollaboratorClientContext collab;
    private HashMap<String, Integer> idToRequestID;
    private HashMap<String, String> idToType;
    private HashMap<Integer, IHttpRequestResponse> requests;
    private int count = 0;

    Correlator() {
        idToRequestID = new HashMap<>();
        requests = new HashMap<>();
        idToType = new HashMap<>();
        collab = Utilities.callbacks.createBurpCollaboratorClientContext();
    }

    java.util.List<IBurpCollaboratorInteraction> poll() {
        return collab.fetchAllCollaboratorInteractions();
    }

    Integer addRequest(IHttpRequestResponse messageInfo) {
        Integer requestCode = count++;
        requests.put(requestCode, messageInfo);
        return requestCode;
    }

    String generateCollabId(int requestCode, String type) {
        String id = collab.generatePayload(false);
        idToRequestID.put(id, requestCode);
        idToType.put(id, type);
        return id+"."+collab.getCollaboratorServerLocation();
    }

    IHttpRequestResponse getRequest(String collabId) {
        int requestId = idToRequestID.get(collabId);
        return requests.get(requestId);
    }

    String getType(String collabid) {
        return idToType.get(collabid);
    }
}

class Injector implements IHttpListener {

    private Correlator collab;

    Injector(Correlator collab) {
        this.collab = collab;
    }

    public byte[] injectPayloads(byte[] request, Integer requestCode) {
        byte[] fixed;

        fixed = Utilities.addOrReplaceHeader(request, "User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36 http://"+collab.generateCollabId(requestCode, "User-Agent"));

        fixed = Utilities.addOrReplaceHeader(fixed, "Referer", "http://"+collab.generateCollabId(requestCode, "Referer"));

        fixed = Utilities.addOrReplaceHeader(fixed, "X-Wap-Profile", "http://"+collab.generateCollabId(requestCode, "WAP")+"/wap.xml");

        fixed = Utilities.addOrReplaceHeader(fixed, "Contact", "user@"+collab.generateCollabId(requestCode, "Contact"));

        // add or overwrite: X-Forwarded-Host, Origin
        return fixed;
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        if (!messageIsRequest) {
            return;
        }

        Integer requestCode = collab.addRequest(messageInfo);

        messageInfo.setRequest(injectPayloads(messageInfo.getRequest(), requestCode));


    }

}