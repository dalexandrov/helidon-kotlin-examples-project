/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.examples.integrations.cdi.jpa

import java.net.URI
import java.util.*
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.transaction.Status
import javax.transaction.SystemException
import javax.transaction.Transaction
import javax.transaction.Transactional
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A JAX-RS root resource class that manipulates greetings in a
 * database.
 *
 * @see .get
 * @see .post
 */
@Path("")
@RequestScoped
open class HelloWorldResource
/**
 * Creates a new [HelloWorldResource].
 */
{
    /**
     * The [EntityManager] used by this class.
     *
     *
     * Note that it behaves as though there is a transaction manager
     * in effect, because there is.
     */
    @PersistenceContext(unitName = "test")
    private lateinit var entityManager: EntityManager

    /**
     * A [Transaction] that is guaranteed to be non-`null`
     * only when a transactional method is executing.
     *
     * @see .post
     */
    @Inject
    private lateinit var transaction: Transaction

    /**
     * Returns a [Response] with a status of `404` when
     * invoked.
     *
     * @return a non-`null` [Response]
     */
    @get:Path("favicon.ico")
    @get:GET
    open val favicon: Response
        get() = Response.status(404).build()

    /**
     * When handed a [String] like, say, "`hello`", responds
     * with the second part of the composite greeting as found via an
     * [EntityManager].
     *
     * @param firstPart the first part of the greeting; must not be
     * `null`
     *
     * @return the second part of the greeting; never `null`
     *
     * encountered an error
     */
    @GET
    @Path("{firstPart}")
    @Produces(MediaType.TEXT_PLAIN)
    open operator fun get(@PathParam("firstPart") firstPart: String): String {
        Objects.requireNonNull(firstPart)
        val greeting = entityManager.find(Greeting::class.java, firstPart)!!
        return greeting.toString()
    }

    /**
     * When handed two parts of a greeting, like, say, "`hello`"
     * and "`world`", stores a new [Greeting] entity in the
     * database appropriately.
     *
     * @param firstPart the first part of the greeting; must not be
     * `null`
     *
     * @param secondPart the second part of the greeting; must not be
     * `null`
     *
     * @return the [String] representation of the resulting [ ]'s identifier; never `null`
     *
     * encountered an error
     *
     * @exception SystemException if something went wrong with the
     * transaction
     */
    @POST
    @Path("{firstPart}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional(Transactional.TxType.REQUIRED)
    @Throws(SystemException::class)
    open fun post(@PathParam("firstPart") firstPart: String,
             secondPart: String): Response {
        Objects.requireNonNull(firstPart)
        Objects.requireNonNull(secondPart)
        assert(transaction.status == Status.STATUS_ACTIVE)
        assert(entityManager.isJoinedToTransaction)
        var greeting = entityManager.find(Greeting::class.java, firstPart)
        val created: Boolean
        if (greeting == null) {
            greeting = Greeting(firstPart, secondPart)
            entityManager.persist(greeting)
            created = true
        } else {
            greeting.setSecondPart(secondPart)
            created = false
        }
        assert(entityManager.contains(greeting))
        return if (created) {
            Response.created(URI.create(firstPart)).build()
        } else {
            Response.ok(firstPart).build()
        }
    }
}