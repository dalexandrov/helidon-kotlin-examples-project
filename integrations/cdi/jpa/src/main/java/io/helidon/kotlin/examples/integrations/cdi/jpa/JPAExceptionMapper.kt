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
package io.helidon.kotlin.examples.integrations.cdi.jpa

import javax.enterprise.context.ApplicationScoped
import javax.persistence.EntityNotFoundException
import javax.persistence.NoResultException
import javax.persistence.PersistenceException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

/**
 * An [ExceptionMapper] that handles [ ]s.
 *
 * @see ExceptionMapper
 */
@ApplicationScoped
@Provider
open class JPAExceptionMapper
/**
 * Creates a new [JPAExceptionMapper].
 */
    : ExceptionMapper<PersistenceException?> {
    /**
     * Returns an appropriate non-`null` [Response] for the
     * supplied [PersistenceException].
     *
     * @param persistenceException the [PersistenceException] that
     * caused this [JPAExceptionMapper] to be invoked; may be
     * `null`
     *
     * @return a non-`null` [Response] representing the
     * error
     */
    override fun toResponse(persistenceException: PersistenceException?): Response? {
        val returnValue: Response?
        if (persistenceException is NoResultException
                || persistenceException is EntityNotFoundException) {
            returnValue = Response.status(404).build()
        } else {
            returnValue = null
            throw persistenceException!!
        }
        return returnValue
    }
}