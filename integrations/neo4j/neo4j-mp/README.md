# Helidon Quickstart MP Example

This example implements a simple Neo4j REST service using MicroProfile. Written in Kotlin.

## Build and run

Bring up a Neo4j instance via Docker

```bash
docker run --publish=7474:7474 --publish=7687:7687 -e 'NEO4J_AUTH=neo4j/secret'  neo4j:4.0
```

Goto the Neo4j browser and play the first step of the movies graph: [`:play movies`](http://localhost:7474/browser/?cmd=play&arg=movies).


Then build with JDK11+
```bash
mvn package
java -jar target/helidon-kotlin-integrations-neo4j-mp.jar
```

## Exercise the application

```
curl -X GET http://localhost:8080/movies

```

## Try health and metrics

```
curl -s -X GET http://localhost:8080/health
{"outcome":"UP",...
. . .

# Prometheus Format
curl -s -X GET http://localhost:8080/metrics
# TYPE base:gc_g1_young_generation_count gauge
. . .

# JSON Format
curl -H 'Accept: application/json' -X GET http://localhost:8080/metrics
{"base":...
. . .

```

## Build the Docker Image

```
docker build -t helidon-integrations-neo4j-mp .
```

## Start the application with Docker

```
docker run --rm -p 8080:8080 helidon-integrations-neo4j-mp:latest
```

Exercise the application as described above

## Deploy the application to Kubernetes

```
kubectl cluster-info                                # Verify which cluster
kubectl get pods                                    # Verify connectivity to cluster
kubectl create -f app.yaml                          # Deploy application
kubectl get service helidon-integrations-neo4j-mp   # Verify deployed service
```