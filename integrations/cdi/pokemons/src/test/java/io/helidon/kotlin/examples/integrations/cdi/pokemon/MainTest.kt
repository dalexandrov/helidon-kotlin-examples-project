/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.integrations.cdi.pokemon

import io.helidon.microprofile.server.Server
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.enterprise.inject.se.SeContainer
import javax.enterprise.inject.spi.CDI
import javax.json.JsonArray
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import org.hamcrest.Matchers.`is` as Is

internal class MainTest {
    @Test
    fun testPokemonTypes() {
        val types = client.target(getConnectionString("/type"))
                .request()
                .get(JsonArray::class.java)
        MatcherAssert.assertThat(types.size, Is(18))
    }

    @Test
    fun testPokemon() {
        MatcherAssert.assertThat(pokemonCount, Is(6))
        var pokemon = client.target(getConnectionString("/pokemon/1"))
                .request()
                .get(Pokemon::class.java)
        MatcherAssert.assertThat(pokemon.name, Is("Bulbasaur"))
        pokemon = client.target(getConnectionString("/pokemon/name/Charmander"))
                .request()
                .get(Pokemon::class.java)
        MatcherAssert.assertThat(pokemon.type, Is(10))
        var response = client.target(getConnectionString("/pokemon/1"))
                .request()
                .get()
        MatcherAssert.assertThat(response.status, Is(200))
        val test = Pokemon()
        test.type = 1
        test.id = 100
        test.name = "Test"
        response = client.target(getConnectionString("/pokemon"))
                .request()
                .post(Entity.entity(test, MediaType.APPLICATION_JSON))
        MatcherAssert.assertThat(response.status, Is(204))
        MatcherAssert.assertThat(pokemonCount, Is(7))
        response = client.target(getConnectionString("/pokemon/100"))
                .request()
                .delete()
        MatcherAssert.assertThat(response.status, Is(204))
        MatcherAssert.assertThat(pokemonCount, Is(6))
    }

    private val pokemonCount: Int
        private get() {
            val pokemons = client.target(getConnectionString("/pokemon"))
                    .request()
                    .get(JsonArray::class.java)
            return pokemons.size
        }

    private fun getConnectionString(path: String): String {
        return "http://localhost:" + server.port() + path
    }

    companion object {
        private lateinit var server: Server
        private lateinit var client: Client

        @BeforeAll
        @JvmStatic
        fun startTheServer() {
            client = ClientBuilder.newClient()
            server = Server.create().start()
        }

        @AfterAll
        @JvmStatic
        fun destroyClass() {
            val current = CDI.current()
            (current as SeContainer).close()
        }
    }
}