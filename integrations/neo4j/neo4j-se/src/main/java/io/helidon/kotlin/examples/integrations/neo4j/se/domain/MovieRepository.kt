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
 *
 */
package io.helidon.kotlin.examples.integrations.neo4j.se.domain

import org.neo4j.driver.Driver
import org.neo4j.driver.Record
import org.neo4j.driver.Transaction
import org.neo4j.driver.Value

/**
 * The Movie repository.
 */
class MovieRepository
/**
 * Constructor for the repo.
 *
 * @param driver
 */(private val driver: Driver) {
    /**
     * Returns all the movies.
     * @return List with movies
     */
    fun findAll(): List<Movie> {
        driver.session().use { session ->
            val query = (""
                    + "match (m:Movie) "
                    + "match (m) <- [:DIRECTED] - (d:Person) "
                    + "match (m) <- [r:ACTED_IN] - (a:Person) "
                    + "return m, collect(d) as directors, collect({name:a.name, roles: r.roles}) as actors")
            return session.readTransaction { tx: Transaction ->
                tx.run(query).list { r: Record ->
                    val movieNode = r["m"].asNode()
                    val directors = r["directors"].asList { v: Value ->
                        val personNode = v.asNode()
                        Person(personNode["born"].asInt(), personNode["name"].asString())
                    }
                    val actors = r["actors"].asList { v: Value -> Actor(v["name"].asString(), v["roles"].asList { obj: Value -> obj.asString() }) }
                    val m = Movie(movieNode["title"].asString(), movieNode["tagline"].asString())
                    m.released = movieNode["released"].asInt()
                    m.setDirectorss(directors)
                    m.actors = actors
                    m
                }
            }
        }
    }
}