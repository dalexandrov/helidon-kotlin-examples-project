/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.integrations.examples.oci.objectstorage.jaxrs

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A JAX-RS resource class rooted at `/logo`.
 *
 * @see .getLogo
 */
@Path("/logo")
@ApplicationScoped
open class HelidonLogoResource @Inject constructor(client: ObjectStorage?,
                                              @ConfigProperty(name = "oci.objectstorage.namespace") namespaceName: String) {
    private val client: ObjectStorage = Objects.requireNonNull(client)!!
    private val namespaceName: String

    /**
     * Returns a non-`null` [Response] which, if successful, will contain the object stored under the supplied `namespaceName`, `bucketName` and `objectName`.
     *
     * @param namespaceName the OCI object storage namespace to use; must not be `null`
     *
     * @param bucketName the OCI object storage bucket name to use; must not be `null`
     *
     * @param objectName the OCI object storage object name to use; must not be `null`
     *
     * @return a non-`null` [Response] describing the operation
     *
     * @exception NullPointerException if any of the parameters is `null`
     */
    @GET
    @Path("/{namespaceName}/{bucketName}/{objectName}")
    @Produces(MediaType.WILDCARD)
    open fun getLogo(@PathParam("namespaceName") namespaceName: String?,
                @PathParam("bucketName") bucketName: String?,
                @PathParam("objectName") objectName: String?): Response? {
        var namespaceName = namespaceName
        val returnValue: Response?
        if (bucketName == null || bucketName.isEmpty() || objectName == null || objectName.isEmpty()) {
            returnValue = Response.status(400)
                    .build()
        } else {
            if (namespaceName == null || namespaceName.isEmpty()) {
                namespaceName = this.namespaceName
            }
            var temp: Response? = null
            temp = try {
                val request = GetObjectRequest.builder()
                        .namespaceName(namespaceName)
                        .bucketName(bucketName)
                        .objectName(objectName)
                        .build()!!
                val response = client.getObject(request)!!
                val contentLength = response.contentLength!!
                if (contentLength <= 0L) {
                    Response.noContent()
                            .build()
                } else {
                    Response.ok()
                            .type(response.contentType)
                            .entity(response.inputStream)
                            .build()
                }
            } catch (bmcException: BmcException) {
                val statusCode = bmcException.statusCode
                Response.status(statusCode)
                        .build()
            } finally {
                returnValue = temp
            }
        }
        return returnValue
    }

    /**
     * Creates a new [HelidonLogoResource].
     *
     * @param client an [ObjectStorage] client; must not be `null`
     *
     * @param namespaceName the name of an OCI object storage namespace that will be used; must not be `null`
     *
     */
    init {
        this.namespaceName = Objects.requireNonNull(namespaceName)
    }
}