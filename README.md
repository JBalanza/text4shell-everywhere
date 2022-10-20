This is a simple fork of PortSwigger's excellent Collaborator Everywhere, with the injection parameters changed to payloads for the critical text4shell CVE-2022-42889 vulnerability. This extension only works on in-scope traffic, and works by injecting headers into your proxy traffic with Text4Shell exploits.</p>

To use it, simply install the plugin and browse the target website. Findings will be presented in the 'Issues' tab. You can easily customise injected payloads by editing /resources/injections


A Burp Professional License is required. 

Tool developed by Javier Balanzategui, part of the [Kudelski Security Team](https://www.kudelskisecurity.com)

To build it:
1. Install gradle
2. gradle build dependencies

To use it:
1. Add the site to the Scope tab in burp

A list of potential affected software can be found [here](https://mvnrepository.com/artifact/org.apache.commons/commons-text/usages?p=10)