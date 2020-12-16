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
package io.helidon.kotlin.examples.webclient.standalone

import asSingle
import io.helidon.common.http.Http
import io.helidon.config.Config
import io.helidon.webserver.*
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import java.util.logging.Logger
import javax.json.Json
import javax.json.JsonException
import javax.json.JsonObject

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
 * curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
 * The message is returned as a JSON object
 */
class GreetService internal constructor(config: Config) : Service {
    /**
     * The config value for the key `greeting`.
     */
    private val greeting = AtomicReference<String>()

    /**
     * A service registers itself by updating the routing rules.
     *
     * @param rules the routing rules.
     */
    override fun update(rules: Routing.Rules) {
        rules["/", Handler { _: ServerRequest, response: ServerResponse -> getDefaultMessageHandler(response) }]["/redirect", Handler { _: ServerRequest, response: ServerResponse -> redirect(response) }]["/{name}", Handler { request: ServerRequest, response: ServerResponse -> getMessageHandler(request, response) }]
                .put("/greeting", Handler { request: ServerRequest, response: ServerResponse -> updateGreetingHandler(request, response) })
    }

    /**
     * Return a worldly greeting message.
     *
     * @param response the server response
     */
    private fun getDefaultMessageHandler(response: ServerResponse) {
        sendResponse(response, "World")
    }

    /**
     * Return a status code of [Http.Status.MOVED_PERMANENTLY_301] and the new location where should
     * client redirect.
     *
     * @param response the server response
     */
    private fun redirect(response: ServerResponse) {
        response.headers().add(Http.Header.LOCATION, "http://localhost:" + ServerMain.serverPort + "/greet/")
        response.status(Http.Status.MOVED_PERMANENTLY_301).send()
    }

    /**
     * Return a greeting message using the name that was provided.
     * @param request the server request
     * @param response the server response
     */
    private fun getMessageHandler(request: ServerRequest,
                                  response: ServerResponse) {
        val name = request.path().param("name")
        sendResponse(response, name)
    }

    /**
     * Set the greeting to use in future messages.
     *
     * @param request  the server request
     * @param response the server response
     */
    private fun updateGreetingHandler(request: ServerRequest,
                                      response: ServerResponse) {
        request.content().asSingle(JsonObject::class.java)
                .thenAccept { jo: JsonObject -> updateGreetingFromJson(jo, response) }
                .exceptionally { ex: Throwable -> processErrors(ex, response) }
    }

    private fun sendResponse(response: ServerResponse, name: String) {
        val msg = String.format("%s %s!", greeting.get(), name)
        val returnObject = JSON.createObjectBuilder()
                .add("message", msg)
                .build()
        response.send(returnObject)
    }

    private fun updateGreetingFromJson(jo: JsonObject, response: ServerResponse) {
        if (!jo.containsKey("greeting")) {
            val jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "No greeting provided")
                    .build()
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject)
            return
        }
        greeting.set(jo.getString("greeting"))
        response.status(Http.Status.NO_CONTENT_204).send()
    }

    companion object {
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())
        private val LOGGER = Logger.getLogger(GreetService::class.java.name)
        private fun <T> processErrors(ex: Throwable, response: ServerResponse): T? {
            if (ex.cause is JsonException) {
                LOGGER.log(Level.FINE, "Invalid JSON", ex)
                val jsonErrorObject = JSON.createObjectBuilder()
                        .add("error", "Invalid JSON")
                        .build()
                response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject)
            } else {
                LOGGER.log(Level.FINE, "Internal error", ex)
                val jsonErrorObject = JSON.createObjectBuilder()
                        .add("error", "Internal error")
                        .build()
                response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(jsonErrorObject)
            }
            return null
        }
    }

    init {
        greeting.set(config["app.greeting"].asString().orElse("Ciao"))
    }
}