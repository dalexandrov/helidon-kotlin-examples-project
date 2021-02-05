/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.examples.dbclient.jdbc

import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbExecute
import io.helidon.kotlin.examples.dbclient.common.AbstractPokemonService
import io.helidon.webserver.ServerRequest
import io.helidon.webserver.ServerResponse
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Example service using a database.
 */
class PokemonService internal constructor(dbClient: DbClient) : AbstractPokemonService(dbClient) {
    /**
     * Delete all pokemons.
     *
     * @param request  the server request
     * @param response the server response
     */
    override fun deleteAllPokemons(request: ServerRequest?, response: ServerResponse?) {
        dbClient().execute { exec ->
            exec // this is to show how ad-hoc statements can be executed (and their naming in Tracing and Metrics)
                    .createDelete("DELETE FROM pokemons")
                    .execute()
        }
                .thenAccept { count -> response?.send("Deleted: $count values") }
                .exceptionally { throwable -> response?.let { sendError(throwable, it) } }
    }

    companion object {
        private val LOGGER = Logger.getLogger(PokemonService::class.java.name)
    }

    init {

        // dirty hack to prepare database for our POC
        // MySQL init
        dbClient.execute { handle: DbExecute -> handle.namedDml("create-table") }
                .thenAccept { x: Long? -> println(x) }
                .exceptionally { throwable: Throwable? ->
                    LOGGER.log(Level.WARNING, "Failed to create table, maybe it already exists?", throwable)
                    null
                }
    }
}