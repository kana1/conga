## CONGA - Extensibility model

CONGA utilizes a flexible plugin-based architecture. A good part of the features shipped with CONGA itself is implemented using plugins bundles with it.

For the plugin architecture the Java ServiceLoader concept is used. See the Oracle tutorial [Creating Extensible Applications][oracle-spi] for details.


### CONGA SPI

CONGA allows to provider custom plugins that are applied on generated files:

* **File Header Plugin**: Plugin is used to add a CONGA file header to all generated files. The plugin controls at which position and with which comment syntax the headers are inserted.
* **Validator Plugin**: Plugin validates files after generation to ensure they are syntactically correct.
* **Handlebars Escaping Strategy Plugin**: Plugin allows to define escaping rules for special chars.
* **Post Processor Plugin**: Plugin that operations on a generated file, e.g. to convert it to a binary file.

Theses plugins detect files with certain extensions, and are executed automatically on them.

Other plugins:

* **Mulitply Plugin**: Plugin controls the generation of multiple files from a single file definition.
* **Handlebars Helper Plugin**: Plugin allows to register your own Handlebar helper to define custom handlebar expression usable in the CONGA templates.

See API documentation for the detailed plugin interfaces:

* [CONGA SPI][conga-spi]
* [CONGA SPI for Handlebars][conga-handlebars-spi]


### Built-in plugins

File plugins:

| Plugin name          | File name(s) | File Header | Validator | Escaping | Post Processor |
|----------------------|--------------|:-----------:|:---------:|:--------:|:--------------:|
| `json`               | .json        | X           | X         | X        |                |
| `xml`                | .xml         | X           | X         | X        |                |
| `conf`               | .conf        | X           |           |          |                |
| `unixShellScript`    | .sh          | X           |           |          |                |
| `windowsShellScript` | .bat, .cmd   | X           |           |          |                |


Multiply plugins:

| Plugin name | Description
|-------------|-------------
| `tenant`    | Allows to generate a file for each tenant defined in the environment.


Handlebars Helper plugins: see [Handlebars quickstart][handlebars-quickstart].

Further plugins are provided by separate wcm.io DevOps projects, see [index page][index].


### Writing your own CONGA plugins

To write your own plugin:

1. Create a new maven JAR file project
2. Create your plugin and let it implement one of the CONGA SPI Plugin interfaces
3. Create a ServiceLoader file at `META-INF/services/<interface class name>` and add your plugin class name
4. Add your JAR as plugin dependency to the CONGA maven plugin


[index]: index.html
[handlebars-quickstart]: handlebars-quickstart.html
[oracle-spi]: https://docs.oracle.com/javase/tutorial/ext/basics/spi.html
[conga-spi]: generator/apidocs/io/wcm/devops/conga/generator/spi/package-summary.html
[conga-handlebars-spi]: generator/apidocs/io/wcm/devops/conga/generator/spi/handlebars/package-summary.html