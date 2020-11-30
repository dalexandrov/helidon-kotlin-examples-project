# Helidon MP TLS Example

This examples shows how to configure server TLS using Helidon MP.

Note: This example uses self-signed server certificate!

Written in Kotlin.

## Build and run

```bash
mvn package
java -jar target/helidon-kotlin-examples-microprofile-tls.jar
```
## Exercise the application
```bash
curl -k -X GET https://localhost:8080
```
