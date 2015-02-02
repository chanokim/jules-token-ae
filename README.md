jules-token-ae
==============

JULIE Token Boundary Detector (JTBD)

Introduction
============
JTBD is a ML-based sentence splitter. It can be retrained on supported
training material and is thus neither language nor domain dependent.



Dependencies
============
JTBD is based on a slightly modified version of the machine learning toolkit MALLET (Version 2.0.x). The
necessary libraries are included in the executable JAR (see below) and accessible via the JULIE Nexus artifact manager.


Usage
=====

To run JTBD just run the self-executing jar "jtbd-&lt;version&gt;.jar". This will show the available modes.

Documentation
==============
For further information please refer to the documentation, JTBD-x.pdf.


technical notes
=========

All components offered on the GitHub page are also available as Maven artifacts from our Nexus repository. To make use of it, add the following repository to your pom.xml:
```xml
<repositories>
   <repository>
      <id>julie-nexus</id>
      <name>JULIELab Public Repository</name>
      <url>https://www.coling.uni-jena.de/nexus/content/groups/public-julie-components/</url>
   </repository>
</repositories>
```
To access the repository, you will need to make our Nexus server https certificate known to your maven installation. To do this, please follow these steps:
First extract the certificate "www.coling.uni-jena.de" from your browser (e.g. in FireFox you can get it by visiting the Preferences/Advanced menu, then show the certificates and store the correct one).
Execute this command to import the certificate and enter a password to protect it
keytool -v -alias mavensrv -import -file <STORED CERTIFICATE> -keystore <PATH WHERE YOU WANT IT>/trust.jks

Add the following to your <tt>.bash_rc/.bash_profile</tt> to use this keystore

<code>export MAVEN_OPTS="-Djavax.net.ssl.trustStore=<PATH WHERE YOU WANT IT>/trust.jks"</code>


We suggest using <tt>/Users/&lt;USERNAME&gt;/.m2</tt> as the location of the keystore.
Then, you have access to all publicly available JULIE components. Please refer to the pom.xml files in the respective GitHub repositories for the current Maven coordinates.
