/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.microprofile.examples.openapi.basic

import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.json.Json
import javax.json.JsonObject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A simple JAX-RS resource with OpenAPI annotations to greet you. Examples:
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
 * Get OpenAPI document for the endpoints
 * curl -X GET http://localhost:8080/openapi
 *
 * Note that the output will include not only the annotated endpoints from this
 * class but also an endpoint added by the [SimpleAPIModelReader].
 *
 * The message is returned as a JSON object.
 */
@Path("/greet")
@RequestScoped
open class GreetResource
/**
 * Using constructor injection to get a configuration property.
 * By default this gets the value from META-INF/microprofile-config
 *
 */ @Inject constructor(
        /**
         * The greeting message provider.
         */
        private val greetingProvider: GreetingProvider) {
    /**
     * Return a worldly greeting message.
     *
     * @return [JsonObject]
     */
    @get:Produces(MediaType.APPLICATION_JSON)
    @get:APIResponse(description = "Simple JSON containing the greeting", content = [Content(mediaType = "application/json", schema = Schema(implementation = GreetingMessage::class))])
    @get:Operation(summary = "Returns a generic greeting", description = "Greets the user generically")
    @get:GET
    open val defaultMessage: JsonObject
        get() = createResponse("World")

    /**
     * Return a greeting message using the name that was provided.
     *
     * @param name the name to greet
     * @return [JsonObject]
     */
    @Path("/{name}")
    @GET
    @Operation(summary = "Returns a personalized greeting")
    @APIResponse(description = "Simple JSON containing the greeting", content = [Content(mediaType = "application/json", schema = Schema(implementation = GreetingMessage::class))])
    @Produces(MediaType.APPLICATION_JSON)
    open fun getMessage(@PathParam("name") name: String): JsonObject {
        return createResponse(name)
    }

    /**
     * Set the greeting to use in future messages.
     *
     * @param jsonObject JSON containing the new greeting
     * @return [Response]
     */
    @Path("/greeting")
    @PUT
    @Operation(summary = "Set the greeting prefix", description = "Permits the client to set the prefix part of the greeting (\"Hello\")")
    @RequestBody(name = "greeting", description = "Conveys the new greeting prefix to use in building greetings", content = [Content(mediaType = "application/json", schema = Schema(implementation = GreetingMessage::class), examples = [ExampleObject(name = "greeting", summary = "Example greeting message to update", value = "New greeting message")])])
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    open fun updateGreeting(jsonObject: JsonObject): Response {
        if (!jsonObject.containsKey("greeting")) {
            val entity = JSON.createObjectBuilder()
                    .add("error", "No greeting provided")
                    .build()
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build()
        }
        val newGreeting = jsonObject.getString("greeting")
        greetingProvider.setMessage(newGreeting)
        return Response.status(Response.Status.NO_CONTENT).build()
    }

    private fun createResponse(who: String): JsonObject {
        val msg = String.format("%s %s!", greetingProvider.getMessage(), who)
        return JSON.createObjectBuilder()
                .add("message", msg)
                .build()
    }

    /**
     * POJO defining the greeting message content exchanged with clients.
     */
    class GreetingMessage {
        /**
         * Gets the message value.
         *
         * @return message value
         */
        /**
         * Sets the message value.
         *
         */
        var message: String? = null
    }

    companion object {
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())
    }
}