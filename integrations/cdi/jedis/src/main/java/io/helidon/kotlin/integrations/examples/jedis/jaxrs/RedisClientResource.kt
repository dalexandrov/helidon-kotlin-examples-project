/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.integrations.examples.jedis.jaxrs

import redis.clients.jedis.Jedis
import java.util.Objects.requireNonNull
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.inject.Provider
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * A JAX-RS resource class rooted at `/jedis`.
 *
 * @see .get
 * @see .set
 * @see .del
 */
@Path("/jedis")
@ApplicationScoped
open class RedisClientResource @Inject constructor(clientProvider: Provider<Jedis>) {
    private val clientProvider: Provider<Jedis> = requireNonNull(clientProvider)

    /**
     * Returns a non-`null` [Response] which, if successful,
     * will contain any value indexed under the supplied Redis key.
     *
     *
     * This method never returns `null`.
     *
     * @param key the key whose value should be deleted; must not be
     * `null`
     *
     * @return a non-`null` [Response]
     *
     * @see .set
     * @see .del
     */
    @GET
    @Path("/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    open operator fun get(@PathParam("key") key: String?): Response {
        val returnValue: Response
        returnValue = if (key == null || key.isEmpty()) {
            Response.status(400)
                    .build()
        } else {
            val response = clientProvider.get()[key]
            if (response == null) {
                Response.status(404)
                        .build()
            } else {
                Response.ok()
                        .entity(response)
                        .build()
            }
        }
        return returnValue
    }

    /**
     * Sets a value under a key in a Redis system.
     *
     * @param uriInfo a [UriInfo] describing the current request;
     * must not be `null`
     *
     * @param key the key in question; must not be `null`
     *
     * @param value the value to set; may be `null`
     *
     * @return a non-`null` [Response] indicating the status
     * of the operation
     *
     * @exception NullPointerException if `uriInfo` is `null`
     *
     * @see .del
     */
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.TEXT_PLAIN)
    open operator fun set(@Context uriInfo: UriInfo,
                     @PathParam("key") key: String?,
                     value: String?): Response {
        requireNonNull(uriInfo)
        val returnValue: Response
        returnValue = if (key == null || key.isEmpty() || value == null) {
            Response.status(400)
                    .build()
        } else {
            val priorValue: Any? = clientProvider.get().getSet(key, value)
            if (priorValue == null) {
                Response.created(uriInfo.requestUri)
                        .build()
            } else {
                Response.ok()
                        .build()
            }
        }
        return returnValue
    }

    /**
     * Deletes a value from Redis.
     *
     * @param key the key identifying the value to delete; must not be
     * `null`
     *
     * @return a non-`null` [Response] describing the result
     * of the operation
     *
     * @see .get
     * @see .set
     */
    @DELETE
    @Path("/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    open fun del(@PathParam("key") key: String?): Response {
        val returnValue: Response
        returnValue = if (key == null || key.isEmpty()) {
            Response.status(400)
                    .build()
        } else {
            val numberOfKeysDeleted = clientProvider.get().del(key)
            if (numberOfKeysDeleted == null || numberOfKeysDeleted.toLong() <= 0L) {
                Response.status(404)
                        .build()
            } else {
                Response.noContent()
                        .build()
            }
        }
        return returnValue
    }

}