# Helidon SE integration with Neo4J example

Written in Kotlin.

## Build and run

Bring up a Neo4j instance via Docker

```bash
docker run --publish=7474:7474 --publish=7687:7687 -e 'NEO4J_AUTH=neo4j/secret'  neo4j:4.0
```

Goto the Neo4j browser and play the first step of the movies graph: [`:play movies`](http://localhost:7474/browser/?cmd=play&arg=movies).

Build and run with With JDK11+
```bash
mvn package
java -jar target/helidon-integrations-neo4j-se.jar  
```

Then access the rest API like this:

````
curl localhost:8080/api/movies
````

#Health and metrics

Heo4jSupport provides health checks and metrics reading from Neo4j.

To enable them add to routing:
```java
// metrics
Neo4jMetricsSupport.builder()
        .driver(neo4j.driver())
        .build()
        .initialize();
// health checks
HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks())   // Adds a convenient set of checks
                .addReadiness(Neo4jHealthCheck.create(neo4j.driver()))
                .build();

return Routing.builder()
        .register(health)                   // Health at "/health"
        .register(metrics)                  // Metrics at "/metrics"
        .register(movieService)
        .build();
```
and enable them in the driver:
```yaml
  pool:
    metricsEnabled: true
```


````
curl localhost:8080/health
````

````
curl localhost:8080/metrics
````



## Build the Docker Image

```
docker build -t helidon-integrations-heo4j-se .
```

## Start the application with Docker

```
docker run --rm -p 8080:8080 helidon-integrations-heo4j-se:latest
```

Exercise the application as described above

## Deploy the application to Kubernetes

```
kubectl cluster-info                                 # Verify which cluster
kubectl get pods                                     # Verify connectivity to cluster
kubectl create -f app.yaml                           # Deply application
kubectl get service helidon-integrations-heo4j-se    # Get service info
```