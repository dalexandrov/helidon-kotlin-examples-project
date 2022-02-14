/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.integrations.vault.hcp.cdi

import io.helidon.integrations.vault.secrets.kv1.Kv1Secrets
import io.helidon.integrations.vault.secrets.kv1.Kv1SecretsRx
import io.helidon.integrations.vault.sys.Sys
import java.util.Map
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.Response

/**
 * JAX-RS resource for Key/Value version 1 secrets engine operations.
 */
@Path("/kv1")
open class Kv1Resource @Inject internal constructor(private val sys: Sys, private val secrets: Kv1Secrets) {
    /**
     * Enable the secrets engine on the default path.
     *
     * @return response
     */
    @Path("/engine")
    @GET
    open fun enableEngine(): Response {
        val response = sys.enableEngine(Kv1SecretsRx.ENGINE)
        return Response.ok()
            .entity("Key/value version 1 secret engine is now enabled. Original status: " + response.status().code())
            .build()
    }

    /**
     * Disable the secrets engine on the default path.
     * @return response
     */
    @Path("/engine")
    @DELETE
    open fun disableEngine(): Response {
        val response = sys.disableEngine(Kv1SecretsRx.ENGINE)
        return Response.ok()
            .entity("Key/value version 1 secret engine is now disabled. Original status: " + response.status().code())
            .build()
    }

    /**
     * Create a secret from request entity, the name of the value is `secret`.
     *
     * @param path path of the secret taken from request path
     * @param secret secret from the entity
     * @return response
     */
    @POST
    @Path("/secrets/{path: .*}")
    open fun createSecret(@PathParam("path") path: String, secret: String): Response {
        val response = secrets.create(path, Map.of("secret", secret))
        return Response.ok()
            .entity(
                "Created secret on path: " + path + ", key is \"secret\", original status: " + response.status().code()
            )
            .build()
    }

    /**
     * Delete the secret on a specified path.
     *
     * @param path path of the secret taken from request path
     * @return response
     */
    @DELETE
    @Path("/secrets/{path: .*}")
    open fun deleteSecret(@PathParam("path") path: String): Response {
        val response = secrets.delete(path)
        return Response.ok()
            .entity("Deleted secret on path: " + path + ". Original status: " + response.status().code())
            .build()
    }

    /**
     * Get the secret on a specified path.
     *
     * @param path path of the secret taken from request path
     * @return response
     */
    @GET
    @Path("/secrets/{path: .*}")
    open fun getSecret(@PathParam("path") path: String?): Response {
        val secret = secrets[path]
        return if (secret.isPresent) {
            val kv1Secret = secret.get()
            Response.ok()
                .entity("Secret: " + secret.get().values().toString())
                .build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }
}