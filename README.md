
# Helidon Examples in Kotlin

Welcome to the Helidon Examples written in Kotlin! 

Our examples are Maven projects and can be built and run with Java 11 -- so make sure you have those:

```
java -version
mvn -version
```

# Building an Example

Each example has a `README` that you will follow. To build most examples
just `cd` to the directory and run `mvn package`:

```
cd kotlin-examples/quickstarts/helidon-kotlin-standalone-quickstart-se
mvn package
```
Usually you can then run the example using:

```
mvn exec:java
```

or
```shell
java -jar target/helidon-kotlin-standalone-quickstart-se.jar
```
But always take a look the example's `README` for details.
