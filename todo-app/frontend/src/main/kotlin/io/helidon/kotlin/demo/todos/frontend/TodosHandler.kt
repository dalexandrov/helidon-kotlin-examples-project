/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

import asSingle
import io.helidon.common.http.Http
import io.helidon.metrics.RegistryFactory
import io.helidon.security.SecurityContext
import io.helidon.webserver.*
import io.opentracing.Span
import org.eclipse.microprofile.metrics.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import javax.json.JsonArray
import javax.json.JsonObject

/**
 * Handler of requests to process TODOs.
 *
 * A TODO is structured as follows:
 * `{ 'title': string, 'completed': boolean, 'id': string }`
 *
 * The IDs are server generated on the initial POST operation (so they are not
 * included in that case).
 *
 * Here is a summary of the operations:
 * `GET /api/todo`: Get all TODOs
 * `GET /api/todo/{id}`: Get a TODO
 * `POST /api/todo`: Create a new TODO, TODO with generated ID
 * is returned
 * `DELETE /api/todo/{id}`: Delete a TODO, deleted TODO is returned
 * `PUT /api/todo/{id}`: Update a TODO,  updated TODO is returned
 */
class TodosHandler(bsc: BackendServiceClient) : Service {
    /**
     * The backend service client.
     */
    private val bsc: BackendServiceClient

    /**
     * Create metric counter.
     */
    private val createCounter: Counter

    /**
     * Update metric counter.
     */
    private val updateCounter: Counter

    /**
     * Delete metric counter.
     */
    private val deleteCounter: Counter
    private fun counterMetadata(name: String, description: String): Metadata {
        return Metadata.builder()
                .withName(name)
                .withDisplayName(name)
                .withDescription(description)
                .withType(MetricType.COUNTER)
                .withUnit(MetricUnits.NONE)
                .build()
    }

    override fun update(rules: Routing.Rules) {
        rules["/todo/{id}", Handler { req: ServerRequest, res: ServerResponse -> getSingle(req, res) }]
                .delete("/todo/{id}", Handler { req: ServerRequest, res: ServerResponse -> delete(req, res) })
                .put("/todo/{id}", Handler { req: ServerRequest, res: ServerResponse -> this.update(req, res) })["/todo", Handler { req: ServerRequest, res: ServerResponse -> getAll(req, res) }]
                .post("/todo", Handler { req: ServerRequest, res: ServerResponse -> create(req, res) })
    }

    /**
     * Handler for `POST /todo`.
     *
     * @param req the server request
     * @param res the server response
     */
    private fun create(req: ServerRequest, res: ServerResponse) {
        secure(req, res) { sc: SecurityContext? ->
            json(req, res) { json: JsonObject? ->
                createCounter.inc()
                bsc.create(json!!, sc)
                        .thenAccept { created: Optional<JsonObject> -> sendResponse(res, created, Http.Status.INTERNAL_SERVER_ERROR_500) }
                        .exceptionally { t: Throwable -> sendError(t, res) }
            }
        }
    }

    /**
     * Handler for `GET /todo`.
     *
     * @param req the server request
     * @param res the server response
     */
    private fun getAll(req: ServerRequest, res: ServerResponse) {
        val createdSpan = AtomicReference<Span>()
        val spanContext = req.spanContext().orElseGet {
            val mySpan = req.tracer().buildSpan("getAll").start()
            createdSpan.set(mySpan)
            mySpan.context()
        }
        secure(req, res) {
            bsc.getAll(spanContext)
                    .thenAccept { t: JsonArray? -> res.send(t) }
                    .exceptionally { t: Throwable -> sendError(t, res) }
                    .whenComplete { _: Void?, _: Throwable? ->
                        val mySpan = createdSpan.get()
                        mySpan?.finish()
                    }
        }
    }

    /**
     * Handler for `PUT /todo/id`.
     *
     * @param req the server request
     * @param res the server response
     */
    private fun update(req: ServerRequest, res: ServerResponse) {
        secure(req, res) { sc: SecurityContext? ->
            json(req, res) { json: JsonObject? ->
                updateCounter.inc()
                // example of asynchronous processing
                bsc.update(sc,
                        req.path().param("id"), json!!, res)
            }
        }
    }

    /**
     * Handler for `DELETE /todo/id`.
     *
     * @param req the server request
     * @param res the server response
     */
    private fun delete(req: ServerRequest, res: ServerResponse) {
        secure(req, res) {
            deleteCounter.inc()
            bsc.deleteSingle(req.path().param("id"))
                    .thenAccept { json: Optional<JsonObject> -> sendResponse(res, json, Http.Status.NOT_FOUND_404) }
                    .exceptionally { throwable: Throwable -> sendError(throwable, res) }
        }
    }

    /**
     * Handler for `GET /todo/id`.
     *
     * @param req the server request
     * @param res the server response
     */
    private fun getSingle(req: ServerRequest, res: ServerResponse) {
        secure(req, res) {
            bsc.getSingle(req.path().param("id"))
                    ?.thenAccept { found: Optional<JsonObject> -> sendResponse(res, found, Http.Status.NOT_FOUND_404) }
                    ?.exceptionally { throwable: Throwable -> sendError(throwable, res) }
        }
    }

    /**
     * Send a response with a `500` status code.
     *
     * @param res the server response
     */
    private fun noSecurityContext(res: ServerResponse) {
        res.status(Http.Status.INTERNAL_SERVER_ERROR_500)
        res.send("Security context not present")
    }

    /**
     * Send the response entity if `jsonResponse` has a value, otherwise
     * sets the status to `failureStatus`.
     *
     * @param res           the server response
     * @param jsonResponse  the response entity
     * @param failureStatus the status to use if `jsonResponse` is empty
     */
    private fun sendResponse(res: ServerResponse,
                             jsonResponse: Optional<out JsonObject>,
                             failureStatus: Http.Status) {
        jsonResponse
                .ifPresentOrElse({ t -> res.send(t) }) { res.status(failureStatus) }
    }

    /**
     * Reads a request entity as {@JsonObject}, and if successful invoke the
     * given consumer, otherwise terminate the request with a `500`
     * status code.
     *
     * @param req  the server request
     * @param res  the server response
     * @param json the `JsonObject` consumer
     */
    private fun json(req: ServerRequest,
                     res: ServerResponse,
                     json: Consumer<JsonObject>) {
        req.content()
                .asSingle(JsonObject::class.java)
                .thenAccept(json)
                .exceptionally { throwable: Throwable -> sendError(throwable, res) }
    }

    /**
     * Reads the request security context, and if successful invoke the given
     * consumer, otherwise terminate the request with a `500`
     * status code.
     *
     * @param req the server request
     * @param res the server response
     * @param ctx the `SecurityContext` consumer
     */
    private fun secure(req: ServerRequest,
                       res: ServerResponse,
                       ctx: Consumer<SecurityContext>) {
        req.context()
                .get(SecurityContext::class.java)
                .ifPresentOrElse(ctx, { noSecurityContext(res) })
    }

    companion object {
        private fun sendError(throwable: Throwable, res: ServerResponse): Void? {
            res.status(Http.Status.INTERNAL_SERVER_ERROR_500)
            res.send(throwable.javaClass.name
                    + ": " + throwable.message)
            return null
        }
    }

    /**
     * Create a new `TodosHandler` instance.
     *
     */
    init {
        val registry = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION)
        this.bsc = bsc
        createCounter = registry.counter("created")
        updateCounter = registry.counter("updates")
        deleteCounter = registry.counter(counterMetadata("deletes",
                "Number of deleted todos"))
    }
}