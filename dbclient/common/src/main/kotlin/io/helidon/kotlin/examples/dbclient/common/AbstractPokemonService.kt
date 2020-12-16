/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.dbclient.common

import asType
import io.helidon.common.http.Http
import io.helidon.common.reactive.Single
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbExecute
import io.helidon.dbclient.DbRow
import io.helidon.dbclient.DbTransaction
import io.helidon.webserver.*
import java.util.*
import java.util.concurrent.CompletionException
import java.util.logging.Level
import java.util.logging.Logger
import javax.json.JsonObject

/**
 * Common methods that do not differ between JDBC and MongoDB.
 */
abstract class AbstractPokemonService
/**
 * Create a new pokemon service with a DB client.
 *
 * @param dbClient DB client to use for database operations
 */ protected constructor(private val dbClient: DbClient) : Service {
    override fun update(rules: Routing.Rules) {
        rules["/", Handler { _: ServerRequest, response: ServerResponse -> listPokemons(response) }] // create new
                .put("/", Handler.create(Pokemon::class.java) { _: ServerRequest, response: ServerResponse, pokemon: Pokemon -> insertPokemon(response, pokemon) }) // update existing
                .post("/{name}/type/{type}", Handler { request: ServerRequest, response: ServerResponse -> insertPokemonSimple(request, response) }) // delete all
                .delete("/", Handler { request: ServerRequest?, response: ServerResponse? -> deleteAllPokemons(request, response) })["/{name}", Handler { request: ServerRequest, response: ServerResponse -> getPokemon(request, response) }] // delete one
                .delete("/{name}", Handler { request: ServerRequest, response: ServerResponse -> deletePokemon(request, response) }) // example of transactional API (local transaction only!)
                .put("/transactional", Handler.create(Pokemon::class.java) { _: ServerRequest, response: ServerResponse, pokemon: Pokemon -> transactional(
                    response,
                    pokemon
                ) }) // update one (TODO this is intentionally wrong - should use JSON request, just to make it simple we use path)
                .put("/{name}/type/{type}", Handler { request: ServerRequest, response: ServerResponse -> updatePokemonType(request, response) })
    }

    /**
     * The DB client associated with this service.
     *
     * @return DB client instance
     */
    protected fun dbClient(): DbClient {
        return dbClient
    }

    /**
     * This method is left unimplemented to show differences between native statements that can be used.
     *
     * @param request Server request
     * @param response Server response
     */
    protected abstract fun deleteAllPokemons(request: ServerRequest?, response: ServerResponse?)

    /**
     * Insert new pokemon with specified name.
     *
     * @param request  the server request
     * @param response the server response
     */
    private fun insertPokemon(response: ServerResponse, pokemon: Pokemon) {
        dbClient.execute { exec: DbExecute ->
            exec
                    .createNamedInsert("insert2")
                    .namedParam(pokemon)
                    .execute()
        }
                .thenAccept { count: Long -> response.send("Inserted: $count values") }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    /**
     * Insert new pokemon with specified name.
     *
     * @param request  the server request
     * @param response the server response
     */
    private fun insertPokemonSimple(request: ServerRequest, response: ServerResponse) {
        // Test Pokemon POJO mapper
        val pokemon = Pokemon(request.path().param("name"), request.path().param("type"))
        dbClient.execute { exec: DbExecute ->
            exec
                    .createNamedInsert("insert2")
                    .namedParam(pokemon)
                    .execute()
        }
                .thenAccept { count: Long -> response.send("Inserted: $count values") }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    /**
     * Get a single pokemon by name.
     *
     * @param request  server request
     * @param response server response
     */
    private fun getPokemon(request: ServerRequest, response: ServerResponse) {
        val pokemonName = request.path().param("name")
        dbClient.execute { exec: DbExecute -> exec.namedGet("select-one", pokemonName) }
                .thenAccept { opt: Optional<DbRow> ->
                    opt.ifPresentOrElse({ sendRow(it, response) }
                    ) {
                        sendNotFound(response, "Pokemon "
                                + pokemonName
                                + " not found")
                    }
                }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    /**
     * Return JsonArray with all stored pokemons or pokemons with matching attributes.
     *
     * @param response the server response
     */
    private fun listPokemons(response: ServerResponse) {
        val rows = dbClient.execute { exec: DbExecute -> exec.namedQuery("select-all") }
                .map { it.asType(JsonObject::class.java) }
        response.send(rows, JsonObject::class.java)
    }

    /**
     * Update a pokemon.
     * Uses a transaction.
     *
     * @param response the server response
     */
    private fun updatePokemonType(request: ServerRequest, response: ServerResponse) {
        val name = request.path().param("name")
        val type = request.path().param("type")
        dbClient.execute { exec: DbExecute ->
            exec
                    .createNamedUpdate("update")
                    .addParam("name", name)
                    .addParam("type", type)
                    .execute()
        }
                .thenAccept { count: Long -> response.send("Updated: $count values") }
                .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    private fun transactional(response: ServerResponse, pokemon: Pokemon) {
        dbClient.inTransaction { tx: DbTransaction ->
            tx
                    .createNamedGet("select-for-update")
                    .namedParam(pokemon)
                    .execute()
                    .flatMapSingle { maybeRow: Optional<DbRow?> ->
                        maybeRow.map {
                            tx.createNamedUpdate("update")
                                    .namedParam(pokemon).execute()
                        }
                                .orElseGet { Single.just(0L) }
                    }
        }.thenAccept { count: Long -> response.send("Updated $count records") }
    }

    /**
     * Delete pokemon with specified name (key).
     *
     * @param request  the server request
     * @param response the server response
     */
    private fun deletePokemon(request: ServerRequest, response: ServerResponse) {
        val name = request.path().param("name")
        dbClient.execute { exec: DbExecute -> exec.namedDelete("delete", name) }
                .thenAccept { count: Long -> response.send("Deleted: $count values") }
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
        response.send(row.asType(JsonObject::class.java))
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
    protected fun <T> sendError(throwable: Throwable, response: ServerResponse): T? {
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
        private val LOGGER = Logger.getLogger(AbstractPokemonService::class.java.name)
    }
}