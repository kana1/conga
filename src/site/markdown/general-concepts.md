## CONGA - General concepts

### Targets System Configuration

The configuration generator focuses on **system configuration** that is usually defined at deployment time and is static at runtime.

It is not targeted to “runtime configuration” like site configuration, tenant configuration that can be changed at any time by authorized users.

This tool is **not a deployment automation tool**. It focuses only on configuration generation and can be integrated in a manual or automated deployment process.


### Supported Systems

The Tool is **not limited to a specific type of application** or runtime environment, any system that relies on system configuration stored somewhere can be provisioned with this tool

Typical target systems we had in mind when designing the tool are: AEM, AEM Dispatcher, Apache Tomcat and Apache HTTPd.

The tool **only generates files**. Further distribution of these files (e.g. copy to server, deploy via HTTP etc.) is not part of the tool.

It is possible to apply this tool only to **parts of a systems configuration**, e.g. generate only the virtual host definitions of a webserver and maintain the other part of the configuration in other ways


### Files that can be generated

Examples:

* Plain text files e.g. Properties, Scripts, Dispatcher, HTTPd config
* JSON files
* XML files
* OSGi configuration snippets
* Sling Provisioning Model
* AEM Content Package containing OSGi configurations

Basically all text-based files types can be generated. And with post processor plugins even binary files.


### Configuration definition model

![Configuration definition model](images/configuration-definition-model.png)

#### DEV terminology

* **Node Role**: A set of functionality/application part that can be deployed to a node, e.g. “AEM CMS”, “AEM Dispatcher”, “Tomcat Service Layer”
    * **Variant**: Variants of a role with same deployment artifacts but different configuration; e.g. “Author”, “Publish”, “Importer”.
    * **Configuration Parameter**: Definition of configuration parameters for that operation can define configuration values that are inserted into the file templates when generating the configuration.
    * **File**: Defines file to be generated for Role/Variant based on a File Template
* **Tenant Role**: Allows to define features required for a tenant, e.g. Tenant Website with or without additional applications like car configurator
* **File Template**: Script-based template the contains static configuration parts and placeholders for configuration values defined by operations


#### OPS terminology

* **Environment**: Environment for a specific project or group of projects with a selection of nodes that work together, e.g. “Market Website Prod”, “Market Website T&I” etc.
* **Node**: A system to deploy to, e.g. a physical machine, virtual machine, Docker container or any other deployment target.
    * For each Node multiple Roles can be assigned; for each Role one Variant
* **Tenant**: A tenant needs a special subset of system configuration parameters, e.g. an own virtual host definition with the tenants public domain name in the Apache webserver
    * For each Tenant multiple Roles can be assigned
* **Configuration Value**: Configuration value for a Configuration parameter in context of environments, nodes, roles and tenants.

### Technology stack

* Implemented in Java 8
* Definition and Configuration Files in [YAML 1.1](http://yaml.org/)
* Template script language: [Handlebars for Java](https://github.com/jknack/handlebars.java)
* Runnable from [Apache Maven](http://maven.apache.org/) or standalone