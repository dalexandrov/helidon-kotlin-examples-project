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
package io.helidon.kotlin.security.examples.jersey

import io.helidon.security.SecurityContext
import io.helidon.security.annotations.Authenticated
import java.util.function.Consumer
import javax.annotation.security.RolesAllowed
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Resources are contained here.
 */
internal object JerseyResources {
    private var port = 0
    fun setPort(port: Int) {
        JerseyResources.port = port
    }

    /**
     * JAX-RS Resource.
     */
    @Path("/")
    open class HelloWorldResource {
        /**
         * Not authenticated resource. All resources will be authorized though (to support path based authorizers,
         * that may have public and protected paths).
         *
         * @param securityContext Security component's context
         * @return Description and current subject
         */
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        open fun getHello(@Context securityContext: SecurityContext): String {
            return ("To test this example, call /protected. If you use a user without \"user\" role, your request will be denied. "
                    + "Your current subject: " + securityContext.user().orElse(SecurityContext.ANONYMOUS))
        }

        /**
         * Returns a hello world message and current subject.
         *
         * @param securityContext Security component's context
         * @return returns hello and subject.
         */
        //this is the important annotation for authentication to kick in
        @Authenticated
        @RolesAllowed("user")
        @Path("/protected")
        @GET
        @Produces(MediaType.TEXT_PLAIN) // due to Jersey approach to path matching, we need two methods to match both the "root" and "root" + subpaths
        open fun getHelloName(@Context securityContext: SecurityContext): String {
            return "Hello, your current subject: " + securityContext.user().orElse(SecurityContext.ANONYMOUS)
        }
    }

    /**
     * JAX-RS Resource demonstrating outbound security.
     */
    @Path("/outbound")
    open class OutboundSecurityResource {
        /**
         * Propagates identity - will explicitly call [HelloWorldResource] on a URI
         * that would resolve to user "tomas", but we will get the current subject...
         *
         * @param securityContext Security component context
         * @return returns hello and subject.
         */
        @Authenticated //this is the important annotation for authentication to kick in
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        open fun propagateIdentity(@Context securityContext: SecurityContext): String {
            val response = ClientBuilder.newBuilder()
                    .build()
                    .target("http://localhost:" + port + "/rest/protected")
                    .request()
                    .get(String::class.java)
            return """
                 Hello, your current subject: ${securityContext.user().orElse(SecurityContext.ANONYMOUS)}
                 Response from hello world: $response
                 """.trimIndent()
        }
    }

    /**
     * JAX-RS Resource protected by hand.
     */
    @Path("/")
    open class HelloWorldProgrammaticResource {
        /**
         * Not authenticated resource. All resources will be authorized though (to support path based authorizers,
         * that may have public and protected paths).
         *
         * @param securityContext Security component's context
         * @return Description and current subject
         */
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        open fun getHello(@Context securityContext: SecurityContext): String {
            return ("To test this example, call /protected. If you use a user without \"user\" role, your request will be denied. "
                    + "Your current subject: " + securityContext.user().orElse(SecurityContext.ANONYMOUS))
        }

        /**
         * Returns a hello world message and current subject.
         *
         * @param securityContext Security component's context
         * @param headers         http inbound headers
         * @return returns hello and subject.
         */
        @Path("/protected")
        @GET
        @Produces(MediaType.TEXT_PLAIN) // due to Jersey approach to path matching, we need two methods to match both the "root" and "root" + subpaths
        open fun getHelloName(@Context securityContext: SecurityContext, @Context headers: HttpHeaders?): Response {
            val resp = securityContext.atnClientBuilder().buildAndGet()
            if (resp.status().isSuccess) {
                //and to authorize
                // role provider can be used directly through context
                return if (securityContext.isUserInRole("user")) {
                    Response
                            .ok("Hello, your current subject: " + securityContext.user().orElse(SecurityContext.ANONYMOUS))
                            .build()
                } else {
                    Response.status(Response.Status.FORBIDDEN).build()
                }
            }
            val builder = Response
                    .status(resp.statusCode().orElse(Response.Status.UNAUTHORIZED.statusCode))
            resp.responseHeaders().forEach { (key: String?, value: List<String?>) -> value.forEach(Consumer { hv: String? -> builder.header(key, hv) }) }
            return builder.build()
        }
    }
}