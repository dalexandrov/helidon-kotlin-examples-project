/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.demo.todos.frontend

import io.helidon.common.http.Http
import io.helidon.config.Config
import io.helidon.security.SecurityContext
import io.helidon.security.integration.jersey.client.ClientSecurity
import io.helidon.tracing.jersey.client.ClientTracingFilter
import io.helidon.webserver.ServerResponse
import io.opentracing.SpanContext
import io.opentracing.Tracer
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer
import java.util.*
import java.util.Map
import java.util.concurrent.CompletionStage
import java.util.logging.Level
import java.util.logging.Logger
import javax.json.JsonArray
import javax.json.JsonObject
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.client.InvocationCallback
import javax.ws.rs.core.Response

/**
 * Client to invoke the TODO backend service.
 */
class BackendServiceClient(
        /**
         * JAXRS client.
         */
        private val client: Client, config: Config) {
    /**
     * Configured endpoint for the backend service.
     */
    private val serviceEndpoint: String = config["services.backend.endpoint"].asString().get()

    /**
     * Tracer instance.
     */
    private val tracer: Tracer = GlobalTracer.get()

    /**
     * Retrieve all TODO entries from the backend.
     *
     * @param spanContext `SpanContext` to use
     * @return future with all records
     */
    fun getAll(spanContext: SpanContext?): CompletionStage<JsonArray> {
        val span = tracer.buildSpan("todos.get-all")
                .asChildOf(spanContext)
                .start()
        val result = client.target("$serviceEndpoint/api/backend")
                .request()
                .property(ClientTracingFilter.CURRENT_SPAN_CONTEXT_PROPERTY_NAME, spanContext)
                .rx()
                .get(JsonArray::class.java)

        // I want to finish my span once the result is received, and report error if failed
        result.thenAccept { span.finish() }
                .exceptionally { t: Throwable? ->
                    Tags.ERROR[span] = true
                    span.log(Map.of("event", "error",
                            "error.object", t))
                    LOGGER.log(Level.WARNING,
                            "Failed to invoke getAll() on "
                                    + serviceEndpoint + "/api/backend", t)
                    span.finish()
                    null
                }
        return result
    }

    /**
     * Retrieve the TODO entry identified by the given ID.
     *
     * @param id the ID identifying the entry to retrieve
     * @return retrieved entry as a `JsonObject`
     */
    fun getSingle(id: String): CompletionStage<Optional<JsonObject>>? {
        return client.target("$serviceEndpoint/api/backend/$id")
                .request()
                .rx()
                .get()
                .thenApply { response: Response -> processSingleEntityResponse(response) }
    }

    /**
     * Delete the TODO entry identified by the given ID.
     *
     * @param id the ID identifying the entry to delete
     * @return deleted entry as a `JsonObject`
     */
    fun deleteSingle(id: String): CompletionStage<Optional<JsonObject>> {
        return client
                .target("$serviceEndpoint/api/backend/$id")
                .request()
                .rx()
                .delete()
                .thenApply { response: Response -> processSingleEntityResponse(response) }
    }

    /**
     * Create a new TODO entry.
     *
     * @param json the new entry value to create as `JsonObject`
     * @param sc `SecurityContext` to use
     * @return created entry as `JsonObject`
     */
    fun create(json: JsonObject, sc: SecurityContext?): CompletionStage<Optional<JsonObject>> {
        return client
                .target("$serviceEndpoint/api/backend/")
                .property(ClientSecurity.PROPERTY_CONTEXT, sc)
                .request()
                .rx()
                .post(Entity.json(json))
                .thenApply { response: Response -> processSingleEntityResponse(response) }
    }

    /**
     * Update a TODO entry identifying by the given ID.
     * @param sc `SecurityContext` to use
     * @param id the ID identifying the entry to update
     * @param json the update entry value as `JsonObject`
     * @param res updated entry as `JsonObject`
     */
    fun update(sc: SecurityContext?,
               id: String,
               json: JsonObject,
               res: ServerResponse) {
        client.target("$serviceEndpoint/api/backend/$id")
                .property(ClientSecurity.PROPERTY_CONTEXT, sc)
                .request()
                .buildPut(Entity.json(json))
                .submit(object : InvocationCallback<Response> {
                    override fun completed(response: Response) {
                        if (response.statusInfo.family == Response.Status.Family.SUCCESSFUL) {
                            res.send(response.readEntity(JsonObject::class.java))
                        } else {
                            res.status(response.status)
                        }
                    }

                    override fun failed(throwable: Throwable) {
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500)
                        res.send()
                    }
                })
    }

    /**
     * Wrap the response entity in an `Optional`.
     * @param response `Reponse` to process
     * @return empty optional if response status is `404`, optional of
     * the response entity otherwise
     */
    private fun processSingleEntityResponse(response: Response): Optional<JsonObject> {
        return if (response.statusInfo.toEnum() == Response.Status.NOT_FOUND) {
            Optional.empty()
        } else Optional.of(response.readEntity(JsonObject::class.java))
    }

    companion object {
        /**
         * Client logger.
         */
        private val LOGGER = Logger.getLogger(BackendServiceClient::class.java.name)
    }

}