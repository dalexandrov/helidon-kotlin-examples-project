/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.service.wolt

import io.helidon.common.http.Http
import io.helidon.common.reactive.Single
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbExecute
import io.helidon.dbclient.DbRow
import io.helidon.dbclient.DbTransaction
import io.helidon.webserver.*
import support.toType
import java.util.*
import java.util.concurrent.CompletionException
import java.util.logging.Level
import java.util.logging.Logger
import javax.json.JsonObject

internal open class DeliveryService(
    private val dbClient: DbClient,
    private val cryptoService: CryptoServiceRx,
    private val sendingService: SendingServiceRx
) : Service {
    init {

        //Cleanup on every usage just for the ease of it.
        dbClient().execute { exec: DbExecute ->
            exec
                .createDelete("DROP ALL OBJECTS")
                .execute()
        }.exceptionally { throwable: Throwable? ->
            LOGGER.log(Level.WARNING, "Failed to Drop Database", throwable)
            null
        }.thenRun {
            dbClient.execute { handle: DbExecute -> handle.namedDml("create-table") }
                .exceptionally { throwable: Throwable? ->
                    LOGGER.log(Level.WARNING, "Failed to create table, maybe it already exists?", throwable)
                    null
                }
        }

    }

    override fun update(rules: Routing.Rules) {
        rules
            .get("/", Handler { _: ServerRequest, response: ServerResponse ->
                listDeliveries(
                    response
                )
            }) // create new
            .put(
                "/",
                Handler.create(Delivery::class.java) { _: ServerRequest, response: ServerResponse, delivery: Delivery ->
                    insertDelivery(
                        response,
                        delivery
                    )
                }) // update existing
            .post(
                "/{id}/{food}/{address}/{status}",
                Handler { request: ServerRequest, response: ServerResponse ->
                    insertDeliverySimple(
                        request,
                        response
                    )
                }) // delete all
            .delete(
                "/",
                Handler { _: ServerRequest, response: ServerResponse ->
                    deleteAllDeliveries(
                        response
                    )
                })
            .get("/{address}",
                Handler { request: ServerRequest, response: ServerResponse ->
                    getDelivery(
                        request,
                        response
                    )
                }) // delete one
            .delete(
                "/{id}",
                Handler { request: ServerRequest, response: ServerResponse ->
                    deleteDelivery(
                        request,
                        response
                    )
                }) // example of transactional API (local transaction only!)
            .put(
                "/transactional",
                Handler.create(Delivery::class.java) { _: ServerRequest, response: ServerResponse, delivery: Delivery ->
                    transactional(
                        response,
                        delivery
                    )
                }) // update one (
            .put(
                "/{id}/{food}/{address}/{status}",
                Handler { request: ServerRequest, response: ServerResponse -> updateDelivery(request, response) })
    }

    private fun dbClient(): DbClient {
        return dbClient
    }

    private fun deleteAllDeliveries(response: ServerResponse) {
        dbClient().execute { exec: DbExecute ->
            exec // this is to show how ad-hoc statements can be executed (and their naming in Tracing and Metrics)
                .createDelete("DELETE FROM deliveries")
                .execute()
        }
            .thenAccept { count: Long -> response.send("Deleted: $count values") }
            .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    private fun insertDelivery(response: ServerResponse, delivery: Delivery) {
        dbClient.execute { exec: DbExecute ->
            exec
                .createNamedInsert("insert")
                .namedParam(delivery)
                .execute()
        }
            .thenAccept { count: Long -> response.send("Inserted: $count values") }
            .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    private fun insertDeliverySimple(request: ServerRequest, response: ServerResponse) {
        val delivery = Delivery(
            request.path().param("id"),
            request.path().param("food"),
            request.path().param("address"),
            DeliveryStatus.valueOf(request.path().param("status"))
        )
        cryptoService.encryptSecret(delivery.toString()).thenAccept { e-> sendingService.emitMessage(e) }

        dbClient.execute { exec: DbExecute ->
            exec
                .createNamedInsert("insert2")
                .namedParam(delivery)
                .execute()
        }
            .thenAccept { count: Long -> response.send("Inserted: $count values") }
            .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    private fun getDelivery(request: ServerRequest, response: ServerResponse) {
        val delivery = request.path().param("address")
        dbClient.execute { exec: DbExecute -> exec.namedGet("select-one", delivery) }
            .thenAccept { opt: Optional<DbRow> ->
                opt.ifPresentOrElse(
                    { sendRow(it, response) }
                ) {
                    sendNotFound(
                        response, "Delivery to "
                                + delivery
                                + " not found"
                    )
                }
            }
            .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    private fun listDeliveries(response: ServerResponse) {
        val rows = dbClient.execute { exec: DbExecute -> exec.namedQuery("select-all") }
            .map {
                it.toType(
                    JsonObject::class.java
                )
            }
        response.send(rows, JsonObject::class.java)
    }

    private fun updateDelivery(request: ServerRequest, response: ServerResponse) {
        val id = request.path().param("id")
        val food = request.path().param("food")
        val address = request.path().param("address")
        val status = request.path().param("address")
        dbClient.execute { exec: DbExecute ->
            exec
                .createNamedUpdate("update")
                .addParam("id", id)
                .addParam("food", food)
                .addParam("address", address)
                .addParam("status", status)
                .execute()
        }
            .thenAccept { count: Long -> response.send("Updated: $count values") }
            .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    private fun transactional(response: ServerResponse, delivery: Delivery) {
        dbClient.inTransaction { tx: DbTransaction ->
            tx
                .createNamedGet("select-for-update")
                .namedParam(delivery)
                .execute()
                .flatMapSingle { maybeRow: Optional<DbRow?> ->
                    maybeRow.map {
                        tx.createNamedUpdate("update")
                            .namedParam(delivery).execute()
                    }
                        .orElseGet { Single.just(0L) }
                }
        }.thenAccept { count: Long -> response.send("Updated $count records") }
    }

    private fun deleteDelivery(request: ServerRequest, response: ServerResponse) {
        val id = request.path().param("id")
        dbClient.execute { exec: DbExecute -> exec.namedDelete("delete", id) }
            .thenAccept { count: Long -> response.send("Deleted: $count values") }
            .exceptionally { throwable: Throwable -> sendError(throwable, response) }
    }

    private fun sendNotFound(response: ServerResponse, message: String) {
        response.status(Http.Status.NOT_FOUND_404)
        response.send(message)
    }

    private fun sendRow(row: DbRow, response: ServerResponse) {
        response.send(row.`as`(JsonObject::class.java))
    }

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
        private val LOGGER = Logger.getLogger(DeliveryService::class.java.name)
    }
}