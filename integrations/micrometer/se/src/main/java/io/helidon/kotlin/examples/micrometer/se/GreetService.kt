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
package io.helidon.kotlin.examples.micrometer.se

import io.helidon.common.http.Http
import io.helidon.config.Config
import io.helidon.kotlin.examples.micrometer.se.GreetingMessage.Companion.fromRest
import io.helidon.webserver.Handler
import io.helidon.webserver.Routing
import io.helidon.webserver.ServerRequest
import io.helidon.webserver.ServerResponse
import io.helidon.webserver.Service
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import java.lang.Runnable
import javax.json.Json
import javax.json.JsonObject

/**
 * A simple service to greet you.
 *
 *
 * Examples:
 * <pre>`Get default greeting message:
 * curl -X GET http://localhost:8080/greet
 *
 * Get greeting message for Joe:
 * curl -X GET http://localhost:8080/greet/Joe
 *
 * Change greeting
 * curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
`</pre> *
 * The greeting message is returned as a JSON object.
 *
 *
 */
class GreetService internal constructor(config: Config, getTimer: Timer, personalizedGetCounter: Counter) : Service {
    /**
     * The config value for the key `greeting`.
     */
    private var greeting: String
    private val getTimer: Timer
    private val personalizedGetCounter: Counter

    /**
     * A service registers itself by updating the routine rules.
     * @param rules the routing rules.
     */
    override fun update(rules: Routing.Rules) {
        rules[Handler { req: ServerRequest, resp: ServerResponse? ->
            getTimer.record(
                Runnable { req.next() })
        }]["/", Handler { request: ServerRequest, response: ServerResponse ->
            getDefaultMessageHandler(
                response
            )
        }]["/{name}", Handler { req: ServerRequest, resp: ServerResponse? ->
            personalizedGetCounter.increment()
            req.next()
        }, Handler { request: ServerRequest, response: ServerResponse ->
            getMessageHandler(
                request,
                response
            )
        }] // ...and process them.
            .put(
                "/greeting",
                Handler { request: ServerRequest, response: ServerResponse ->
                    updateGreetingHandler(
                        request,
                        response
                    )
                })
    }

    /**
     * Return a worldly greeting message.
     * @param request the server request
     * @param response the server response
     */
    private fun getDefaultMessageHandler(
        response: ServerResponse
    ) {
        sendResponse(response, "World")
    }

    /**
     * Return a greeting message using the name that was provided.
     * @param request the server request
     * @param response the server response
     */
    private fun getMessageHandler(
        request: ServerRequest,
        response: ServerResponse
    ) {
        val name = request.path().param("name")
        sendResponse(response, name)
    }

    private fun sendResponse(response: ServerResponse, name: String) {
        val msg = GreetingMessage(String.format("%s %s!", greeting, name))
        response.send(msg.forRest())
    }

    private fun updateGreetingFromJson(jo: JsonObject, response: ServerResponse) {
        if (!jo.containsKey(GreetingMessage.JSON_LABEL)) {
            val jsonErrorObject = JSON_BF.createObjectBuilder()
                .add("error", "No greeting provided")
                .build()
            response.status(Http.Status.BAD_REQUEST_400)
                .send(jsonErrorObject)
            return
        }
        greeting = fromRest(jo).message
        response.status(Http.Status.NO_CONTENT_204).send()
    }

    /**
     * Set the greeting to use in future messages.
     * @param request the server request
     * @param response the server response
     */
    private fun updateGreetingHandler(
        request: ServerRequest,
        response: ServerResponse
    ) {
        request.content().`as`(JsonObject::class.java)
            .thenAccept { jo: JsonObject -> updateGreetingFromJson(jo, response) }
    }

    companion object {
        private val JSON_BF = Json.createBuilderFactory(emptyMap<String, Any>())
    }

    init {
        greeting = config["app.greeting"].asString().orElse("Ciao")
        this.getTimer = getTimer
        this.personalizedGetCounter = personalizedGetCounter
    }
}