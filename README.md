OpenMRS Interoperability Layer Module
=====================================

The OpenMRS interoperability layer module is based on the Fast Healthcare Interoperability Resources (FHIR) standard, which is a widely-used framework for exchanging health care data electronically. The interoperability layer module uses FHIR to define the events and data that can be exchanged between different EMR systems. By using FHIR, the interoperability layer module ensures that the data can be easily understood and used by other EMR systems, regardless of their specific implementations.

Conceptual model overview
-------------------------
![AMRS DHP conceptual model drawio](https://user-images.githubusercontent.com/19473115/205644270-4fc7449e-2dde-4c0e-990e-d32835ac7e18.png)


Building from Source
--------------------
You will need to have Java 1.6+ and Maven 2.x+ installed.  Use the command 'mvn package' to 
compile and package the module.  The .omod file will be in the omod/target folder.

Alternatively you can add the snippet provided in the [Creating Modules](https://wiki.openmrs.org/x/cAEr) page to your 
omod/pom.xml and use the mvn command:

    mvn package -P deploy-web -D deploy.path="../../openmrs-1.8.x/webapp/src/main/webapp"

It will allow you to deploy any changes to your web 
resources such as jsp or js files without re-installing the module. The deploy path says 
where OpenMRS is deployed.

Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.

If uploads are not allowed from the web (changable via a runtime property), you can drop the omod
into the ~/.OpenMRS/modules folder.  (Where ~/.OpenMRS is assumed to be the Application 
Data Directory that the running openmrs is currently using.)  After putting the file in there 
simply restart OpenMRS/tomcat and the module will be loaded and started.
