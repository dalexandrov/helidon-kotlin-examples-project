# Helidon MP with Static Content

This example has a simple Hello World rest enpoint, plus
static content that is loaded from the application's classpath.
The configuration for the static content is in the
`microprofile-config.properties` file.

Written in kotlin

## Build and run

```bash
mvn package
java -jar target/helidon-kotlin-examples-microprofile-mp1_1-static-content.jar
```

## Endpoints

|Endpoint    |Description      |
|:-----------|:----------------|
|`helloworld`|Rest endpoint providing a link to the static content|
|`resource.html`|The static content|
