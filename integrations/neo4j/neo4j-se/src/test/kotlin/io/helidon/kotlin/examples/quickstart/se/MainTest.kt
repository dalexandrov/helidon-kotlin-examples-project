/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.kotlin.examples.quickstart.se

/**
 * Main test class for Neo4j Helidon SE quickstarter.
 */
class MainTest {
//    @Test
//    @Ignore
//    @Throws(Exception::class)
//    fun  // Currently ignore. Decide if we need testcontainers.
//            testMovies() {
//        webClient.get()
//                .path("api/movies")
//                .request(JsonArray::class.java)
//                .thenAccept { result: JsonArray -> Assertions.assertEquals("The Matrix", result.getJsonObject(0).getString("title")) }
//                .toCompletableFuture()
//                .get()
//    }
//
//    @Test
//    @Ignore // Currently ignore. Decide if we need testcontainers.
//    @Throws(Exception::class)
//    fun testHealth() {
//        webClient.get()
//                .path("/health")
//                .request()
//                .thenAccept { response: WebClientResponse -> Assertions.assertEquals(200, response.status().code()) }
//                .toCompletableFuture()
//                .get()
//    }
//
//    @Test
//    @Ignore // Currently ignore. Decide if we need testcontainers.
//    @Throws(Exception::class)
//    fun testMetrics() {
//        webClient.get()
//                .path("/metrics")
//                .request()
//                .thenAccept { response: WebClientResponse -> Assertions.assertEquals(200, response.status().code()) }
//                .toCompletableFuture()
//                .get()
//    }
//
//    companion object {
//        @JvmStatic
//        private lateinit var neo4jContainer: Neo4jContainer<*>
//
//        @JvmStatic
//        private lateinit var webServer: WebServer
//
//        @JvmStatic
//        private lateinit var webClient: WebClient
//
//        //@BeforeAll Decide if we need testcontainers.
//        @Throws(Exception::class)
//        private fun startTheServer() {
//            neo4jContainer = Neo4jContainer<Nothing>("neo4j:4.0")
//                    .withAdminPassword("secret")
//            neo4jContainer.start()
//            GraphDatabase.driver(neo4jContainer.boltUrl, AuthTokens.basic("neo4j", "secret")).use { driver ->
//                driver.session().use { session ->
//                    session.writeTransaction { tx: Transaction ->
//                        tx.run("""CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})
//CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})
//CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})
//CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})
//CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})
//CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})
//CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})
//CREATE (JoelS:Person {name:'Joel Silver', born:1952})
//CREATE
//  (Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrix),
//  (Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrix),
//  (Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrix),
//  (Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrix),
//  (LillyW)-[:DIRECTED]->(TheMatrix),
//  (LanaW)-[:DIRECTED]->(TheMatrix),
//  (JoelS)-[:PRODUCED]->(TheMatrix)""").consume()
//                    }
//                }
//            }
//
//            // Don't know how to set this dynamically otherwise in Helidon
//            System.setProperty("neo4j.uri", neo4jContainer.boltUrl)
//            webServer = startServer()
//            val timeout: Long = 2000 // 2 seconds should be enough to start the server
//            val now = System.currentTimeMillis()
//            while (!webServer.isRunning) {
//                Thread.sleep(100)
//                if (System.currentTimeMillis() - now > timeout) {
//                    Assertions.fail<Any>("Failed to start webserver")
//                }
//            }
//            webClient = WebClient.builder()
//                    .baseUri("http://localhost:" + webServer.port())
//                    .addMediaSupport(JsonpSupport.create())
//                    .build()
//        }
//
//        //@AfterAll
//        @Throws(Exception::class)
//        private fun stopServer() {
//            webServer.shutdown()
//                    .toCompletableFuture()[10, TimeUnit.SECONDS]
//        }
//
//        //@AfterAll
//        fun stopNeo4j() {
//            neo4jContainer.stop()
//        }
//    }
}