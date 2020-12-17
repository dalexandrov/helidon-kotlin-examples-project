# Helidon Config Sources Example

This example shows how to load configuration from multiple 
configuration sources, written in Kotlin.

1. [`DirectorySourceExample.kt`](src/main/java/io/helidon/kotlin/config/examples/sources/DirectorySourceExample.kt)
reads configuration from multiple files in a directory by specifying only the directory.
2. [`LoadSourcesExample.kt`](src/main/java/io/helidon/kotlin/config/examples/sources/LoadSourcesExample.kt)
uses _meta-configuration_ files [`conf/meta-config.yaml`](./conf/meta-config.yaml) 
and [`src/main/resources/meta-config.yaml`](./src/main/resources/meta-config.yaml)
which contain not the configuration itself but
_instructions for loading_ the configuration: what type, from where, etc. It also
applies a filter to modify config values whose keys match a certain pattern.
3. [`WithSourcesExample.kt`](src/main/java/io/helidon/kotlin/config/examples/sources/WithSourcesExample.kt)
combines multiple config sources into a single configuration instance (and adds a
filter.

## Build and run

```bash
mvn package
java -jar target/helidon-kotlin-examples-config-sources.jar
```
