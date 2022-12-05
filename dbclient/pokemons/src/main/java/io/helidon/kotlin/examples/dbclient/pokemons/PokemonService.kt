/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.dbclient.pokemons

import to
import io.helidon.common.http.Http
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbExecute
import io.helidon.dbclient.DbRow
import io.helidon.webserver.*
import java.util.*
import java.util.concurrent.CompletionException
import java.util.logging.Level
import java.util.logging.Logger
import javax.json.JsonObject

/**
 * Example service using a database.
 */
class PokemonService internal constructor(private val dbClient: DbClient) : Service {
    override fun update(rules: Routing.Rules) {
        rules["/", Handler { request: ServerRequest, response: ServerResponse -> index(response) }]["/type", Handler { request: ServerRequest, response: ServerResponse -> listTypes(
            response
        ) }]["/pokemon", Handler { request: ServerRequest, response: ServerResponse -> listPokemons(response) }]["/pokemon/name/{name}", Handler { request: ServerRequest, response: ServerResponse -> getPokemonByName(request, response) }]["/pokemon/{id}", Handler { request: ServerRequest, response: ServerResponse -> getPokemonById(request, response) }] // Create new pokemon
                .post("/pokemon", Handler.create(Pokemon::class.java) { request: ServerRequest, response: ServerResponse, pokemon: Pokemon -> insertPokemon(
                    response,
                    pokemon
                ) }) // Update name of existing pokemon
                .put("/pokemon", Handler.create(Pokemon::class.java) { request: ServerRequest, response: ServerResponse, pokemon: Pokemon -> updatePokemon(
                    response,
                    pokemon
                ) }) // Delete pokemon by ID including type relation
                .delete("/pokemon/{id}", Handler { request: ServerRequest, response: ServerResponse -> deletePokemonById(request, response) })
    }

    /**
     * Return index page.
     *
     * @param response the server response
     */
    private fun index(response: ServerResponse) {
        response.send("""Pokemon JDBC Example:
     GET /type                - List all pokemon types
     GET /pokemon             - List all pokemons
     GET /pokemon/{id}        - Get pokemon by id
     GET /pokemon/name/{name} - Get pokemon by name
    POST /pokemon             - Insert new pokemon:
                                {"id":<id>,"name":<name>,"type":<type>}
     PUT /pokemon             - Update pokemon
                                {"id":<id>,"name":<name>,"type":<type>}
  DELETE /pokemon/{id}        - Delete pokemon with specified id
""")
    }

    /**
     * Return JsonArray with all stored pokemons.
     * Pokemon object contains list of all type names.
     * This method is abstract because implementation is DB dependent.
     *
     * @param response the server response
     */
    private fun listTypes(response: ServerResponse) {
        response.send(dbClient.execute { exec: DbExecute -> exec.namedQuery("select-all-types") }
                .map { it.to<JsonObject>() }, JsonObject::class.java)
    }

    /**
     * Return JsonArray with all stored pokemons.
     * Pokemon object contains list of all type names.
     * This method is abstract because implementation is DB dependent.
     *
     * @param response the server response
     */
    private fun listPokemons(response: ServerResponse) {
        response.send(dbClient.execute { exec: DbExecute -> exec.namedQuery("select-all-pokemons") }
                .map { it.to<JsonObject>() }, JsonObject::class.java)
    }

    /**
     * Get a single pokemon by id.
     *
     * @param request  server request
     * @param response server response
     */
    private fun getPokemonById(request: ServerRequest, response: ServerResponse) {
        try {
            val pokemonId = request.path().param("id").toInt()
            dbClient.execute { exec: DbExecute ->
                exec
                        .createNamedGet("select-pokemon-by-id")
                        .addParam("id", pokemonId)
                        .execute()
            }
                    .thenAccept { maybeRow: Optional<DbRow> ->
                        maybeRow
                                .ifPresentOrElse(
                                        { row: DbRow -> sendRow(row, response) }
                                ) { sendNotFound(response, "Pokemon $pokemonId not found") }
                    }
                    .exceptionally { throwable: Throwable -> sendError(throwable, response) }
        } catch (ex: NumberFormatException) {
            sendError<Any>(ex, response)
        }
    }

    /**
     * Get a single pokemon by name.
     *
     * @param request  server request
     * @param response server response
     */
    private fun getPokemonByName(request: ServerRequest, response: ServerResponse) {
        val pokemonName = request.path().param("name")
        dbClient.execute { exec: DbExecute -> exec.namedGet("select-pokemon-by-name", pokemonName) }
                .thenAccept {
                    if (it.isEmpty) {
                        sendNotFound(response, "Pokemon $pokemonName not found")
                    } else {
                        sendRow(it.get(), response)
                    }
                }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    /**
     * Insert new pokemon with specified name.
     *
     * @param response the server response
     */
    private fun insertPokemon(response: ServerResponse, pokemon: Pokemon) {
        dbClient.execute { exec: DbExecute ->
            exec
                    .createNamedInsert("insert-pokemon")
                    .indexedParam(pokemon)
                    .execute()
        }
                .thenAccept { count: Long -> response.send("Inserted: $count values\n") }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    /**
     * Update a pokemon.
     * Uses a transaction.
     *
     * @param response the server response
     */
    private fun updatePokemon(response: ServerResponse, pokemon: Pokemon) {
        dbClient.execute { exec: DbExecute ->
            exec
                    .createNamedUpdate("update-pokemon-by-id")
                    .namedParam(pokemon)
                    .execute()
        }
                .thenAccept { count: Long -> response.send("Updated: $count values\n") }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    /**
     * Delete pokemon with specified id (key).
     *
     * @param request  the server request
     * @param response the server response
     */
    private fun deletePokemonById(request: ServerRequest, response: ServerResponse) {
        try {
            val id = request.path().param("id").toInt()
            dbClient.execute { exec: DbExecute ->
                exec
                        .createNamedDelete("delete-pokemon-by-id")
                        .addParam("id", id)
                        .execute()
            }
                    .thenAccept { count: Long -> response.send("Deleted: $count values\n") }
                    .exceptionally { throwable: Throwable -> sendError(throwable, response) }
        } catch (ex: NumberFormatException) {
            sendError<Any>(ex, response)
        }
    }

    /**
     * Delete pokemon with specified id (key).
     *
     * @param response the server response
     */
    private fun deleteAllPokemons(response: ServerResponse) {
        // Response message contains information about deleted records from both tables
        val sb = StringBuilder()
        // Pokemon must be removed from both PokemonTypes and Pokemons tables in transaction
        dbClient.execute { exec: DbExecute ->
            exec // Execute delete from PokemonTypes table
                    .createDelete("DELETE FROM Pokemons")
                    .execute()
        } // Process response when transaction is completed
                .thenAccept { count: Long -> response.send("Deleted: $count values\n") }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    /**
     * Send a 404 status code.
     *
     * @param response the server response
     * @param message entity content
     */
    private fun sendNotFound(response: ServerResponse, message: String) {
        response.status(Http.Status.NOT_FOUND_404)
        response.send(message)
    }

    /**
     * Send a single DB row as JSON object.
     *
     * @param row row as read from the database
     * @param response server response
     */
    private fun sendRow(row: DbRow, response: ServerResponse) {
        response.send(row.to<JsonObject>())
    }

    /**
     * Send a 500 response code and a few details.
     *
     * @param throwable throwable that caused the issue
     * @param response server response
     * @param <T> type of expected response, will be always `null`
     * @return `Void` so this method can be registered as a lambda
     * with [java.util.concurrent.CompletionStage.exceptionally]
    </T> */
    private fun <T> sendError(throwable: Throwable, response: ServerResponse): T? {
        var realCause: Throwable? = throwable
        if (throwable is CompletionException) {
            realCause = throwable.cause
        }
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500)
        response.send("Failed to process request: " + realCause!!.javaClass.name + "(" + realCause.message + ")")
        LOGGER.log(Level.WARNING, "Failed to process request", throwable)
        return null
    }

    companion object {
        private val LOGGER = Logger.getLogger(PokemonService::class.java.name)
    }
}