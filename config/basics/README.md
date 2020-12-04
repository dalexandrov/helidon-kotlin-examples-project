# Helidon Config Basic Example

This example shows the basics of using Helidon SE Config. The
[Main.kt](src/main/kotlin/io/helidon/kotlin/config/examples/basics/Main.kt) class shows:

* loading configuration from a resource 
[`application.conf`](./src/main/resources/application.conf) on the classpath 
containing config in HOCON (Human-Optimized Config Object Notation) format
* getting configuration values of various types

## Build and run

```bash
mvn package
java -jar target/helidon-kotlin-examples-config-basics.jar
```
