/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.demo.todos.backend

import io.helidon.security.Principal
import io.helidon.security.SecurityContext
import io.helidon.security.Subject
import io.helidon.security.annotations.Authenticated
import io.helidon.security.annotations.Authorized
import java.util.*
import java.util.function.Consumer
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.json.Json
import javax.json.JsonObject
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * The TODO backend REST service.
 */
@Path("/api/backend")
@Authenticated
@Authorized
@ApplicationScoped
class JaxRsBackendResource
/**
 * Create new `JaxRsBackendResource` instance.
 * @param dbs the database service facade to use
 */ @Inject constructor(
        /**
         * The database service facade.
         */
        private val backendService: DbService) {
    /**
     * Retrieve all TODO entries.
     *
     * @param context security context to map the user
     * @param headers HTTP headers
     * @return the response with the retrieved entries as entity
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(@Context context: SecurityContext,
             @Context headers: HttpHeaders?): Response {
        val span = context.tracer().buildSpan("jaxrs:list")
                .asChildOf(context.tracingSpan())
                .start()
        val builder = JSON.createArrayBuilder()
        backendService.list(context.tracingSpan(), getUserId(context))
                .forEach(Consumer { data: Todo -> builder.add(data.forRest()) })
        val response = Response.ok(builder.build()).build()
        span.finish()
        return response
    }

    /**
     * Get the TODO entry identified by the given ID.
     * @param id the ID of the entry to retrieve
     * @param context security context to map the user
     * @return the response with the retrieved entry as entity
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    operator fun get(@PathParam("id") id: String?,
                     @Context context: SecurityContext): Response {
        return backendService[context.tracingSpan(), id!!, getUserId(context)]
                .map { obj: Todo -> obj.forRest() }
                .map { entity: JsonObject? -> Response.ok(entity) }
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build()
    }

    /**
     * Delete the TODO entry identified by the given ID.
     * @param id the id of the entry to delete
     * @param context security context to map the user
     * @return the response with the deleted entry as entity
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun delete(@PathParam("id") id: String?,
               @Context context: SecurityContext): Response {
        return backendService
                .delete(context.tracingSpan(), id!!, getUserId(context))
                .map { obj: Todo -> obj.forRest() }
                .map { entity: JsonObject? -> Response.ok(entity) }
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build()
    }

    /**
     * Create a new TODO entry.
     * @param jsonObject the value of the new entry
     * @param context security context to map the user
     * @return the response (`200` status if successful
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createIt(jsonObject: JsonObject,
                 @Context context: SecurityContext): Response {
        val newId = UUID.randomUUID().toString()
        val userId = getUserId(context)
        val newBackend = Todo.newTodoFromRest(jsonObject, userId, newId)
        backendService.insert(context.tracingSpan(), newBackend)
        return Response.ok(newBackend.forRest()).build()
    }

    /**
     * Update the TODO entry identified by the given ID.
     * @param id the ID of the entry to update
     * @param jsonObject the updated value of the entry
     * @param context security context to map the user
     * @return the response with the updated entry as entity
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun update(@PathParam("id") id: String?,
               jsonObject: JsonObject,
               @Context context: SecurityContext): Response {
        return backendService
                .update(context.tracingSpan(),
                        Todo.fromRest(jsonObject, getUserId(context), id))
                .map { obj: Todo -> obj.forRest() }
                .map { entity: JsonObject? -> Response.ok(entity) }
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build()
    }

    /**
     * Get the user id from the security context.
     * @param context the security context
     * @return user id found in the context or `<ANONYMOUS>` otherwise
     */
    private fun getUserId(context: SecurityContext): String {
        return context.user()
                .map { obj: Subject -> obj.principal() }
                .map { obj: Principal -> obj.id() }
                .orElse("<ANONYMOUS>")
    }

    companion object {
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())
    }
}