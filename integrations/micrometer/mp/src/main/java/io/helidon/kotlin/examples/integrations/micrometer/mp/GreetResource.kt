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
package io.helidon.kotlin.examples.integrations.micrometer.mp

import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.json.Json
import javax.json.JsonObject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A simple JAX-RS resource to greet you. Examples:
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
 * The message is returned as a JSON object.
 */
@Path("/greet")
@RequestScoped
open class GreetResource
/**
 * Using constructor injection to get a configuration property.
 * By default this gets the value from META-INF/microprofile-config
 *
 * @param greetingConfig the configured greeting message
 */ @Inject constructor(
    /**
     * The greeting message provider.
     */
    private val greetingProvider: GreetingProvider
) {
    /**
     * Return a worldly greeting message.
     *
     * @return [JsonObject]
     */
    @get:Timed(
        value = GETS_TIMER_NAME,
        description = GETS_TIMER_DESCRIPTION,
        histogram = true
    )
    @get:Produces(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(value = PERSONALIZED_GETS_COUNTER_NAME, description = PERSONALIZED_GETS_COUNTER_DESCRIPTION)
    @Timed(value = GETS_TIMER_NAME, description = GETS_TIMER_DESCRIPTION, histogram = true)
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequestBody(
        name = "greeting",
        required = true,
        content = [Content(
            mediaType = "application/json",
            schema = Schema(type = SchemaType.STRING, example = "{\"greeting\" : \"Hola\"}")
        )]
    )
    @APIResponses(
        APIResponse(name = "normal", responseCode = "204", description = "Greeting updated"),
        APIResponse(
            name = "missing 'greeting'",
            responseCode = "400",
            description = "JSON did not contain setting for 'greeting'"
        )
    )
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

    companion object {
        const val PERSONALIZED_GETS_COUNTER_NAME = "personalizedGets"
        private const val PERSONALIZED_GETS_COUNTER_DESCRIPTION = "Counts personalized GET operations"
        const val GETS_TIMER_NAME = "allGets"
        private const val GETS_TIMER_DESCRIPTION = "Tracks all GET operations"
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())
    }
}