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
package io.helidon.kotlin.examples.dbclient.mongo

import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbExecute
import io.helidon.kotlin.examples.dbclient.common.AbstractPokemonService
import io.helidon.webserver.ServerRequest
import io.helidon.webserver.ServerResponse

/**
 * A simple service to greet you. Examples:
 *
 * Get default greeting message:
 * curl -X GET http://localhost:8080/greet
 *
 * Get greeting message for Joe:
 * curl -X GET http://localhost:8080/greet/Joe
 *
 * Change greeting
 * curl -X PUT http://localhost:8080/greet/greeting/Hola
 *
 * The message is returned as a JSON object
 */
class PokemonService internal constructor(dbClient: DbClient?) : AbstractPokemonService(dbClient!!) {
    /**
     * Delete all pokemons.
     *
     * @param request  the server request
     * @param response the server response
     */
    override fun deleteAllPokemons(request: ServerRequest?, response: ServerResponse?) {
        dbClient().execute { exec: DbExecute ->
            exec
                    .createNamedDelete("delete-all")
                    .execute()
        }
                .thenAccept { count: Long -> response!!.send("Deleted: $count values") }
                .exceptionally { throwable: Throwable? -> sendError(throwable!!, response!!) }
    }
}