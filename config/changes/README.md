# Helidon Config Changes Example

This example shows how an application can deal with changes to 
configuration written in Kotlin.

## Change notification

The example highlights two approaches to change notification:

1. [`ChangesSubscriberExample.kt`](src/main/kotlin/io/helidon/kotlin/config/examples/changes/ChangesSubscriberExample.kt):
uses `Config.changes` to register an application-specific `Flow.Subscriber` with a 
config-provided `Flow.Publisher` to be notified of changes to the underlying 
configuration source as they occur
2. [`OnChangeExample.kt`](src/main/kotlin/io/helidon/kotlin/config/examples/changes/OnChangeExample.kt):
uses `Config.onChange`, passing either a method reference (a lambda expression
would also work) which the config system invokes when the config source changes
)

## Latest-value supplier

A third example illustrates a different solution. 
Recall that once your application obtains a `Config` instance, its config values 
do not change. The 
[`AsSupplierExample.kt`](src/main/kotlin/io/helidon/kotlin/config/examples/changes/AsSupplierExample.kt)
example shows how your application can get a config _supplier_ that always reports 
the latest config value for a key, including any changes made after your
application obtained the `Config` object. Although this approach does not notify
your application _when_ changes occur, it _does_ permit your code to always use 
the most up-to-date value. Sometimes that is all you need.

## Build and run

```bash
mvn package
java -jar target/helidon-kotlin-examples-config-changes.jar
```
