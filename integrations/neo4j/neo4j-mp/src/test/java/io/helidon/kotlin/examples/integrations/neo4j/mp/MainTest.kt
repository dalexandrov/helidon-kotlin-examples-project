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
package io.helidon.kotlin.examples.integrations.neo4j.mp

/**
 * Main tests of the application done here.
 */
internal class MainTest {
//    @Test
//    @Ignore
//    fun  // Currently ignore. Decide if we need testcontainers.
//            testMovies() {
//        val client = ClientBuilder.newClient()
//        val jsorArray = client
//                .target(getConnectionString("/movies"))
//                .request()
//                .get(JsonArray::class.java)
//        val first = jsorArray.getJsonObject(0)
//        Assertions.assertEquals("The Matrix", first.getString("title"))
//    }
//
//    private fun getConnectionString(path: String): String {
//        return "http://localhost:" + server!!.port() + path
//    }
//
//    companion object {
//        @JvmStatic
//        private lateinit var server: Server?
//
//        @JvmStatic
//        private lateinit var neo4jContainer: Neo4jContainer<*>?
//
//        //@BeforeAll Decide if we need testcontainers.
//        @Throws(Exception::class)
//        fun startTheServer() {
//            neo4jContainer = Neo4jContainer<Nothing>("neo4j:4.0")
//                    .withAdminPassword("secret")
//            neo4jContainer.start()
//            server = Server.create().start()
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
//            System.setProperty("neo4j.uri", neo4jContainer!!.boltUrl)
//        }
//
//        //@AfterAll
//        fun destroyClass() {
//            val current = CDI.current()
//            (current as SeContainer).close()
//        }
//
//        //@AfterAll
//        fun stopNeo4j() {
//            neo4jContainer.stop()
//        }
//    }
}