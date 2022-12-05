/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.microprofile.example.security

import io.helidon.security.Security
import io.helidon.security.SecurityContext
import io.helidon.security.annotations.Authenticated
import io.helidon.security.annotations.Authorized
import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

/**
 * A dynamic resource that shows a link to the static resource.
 */
@Path("/helloworld")
@RequestScoped
open class HelloWorldResource {
    @Inject
    private val security: Security? = null

    @Inject
    private val securityContext: SecurityContext? = null

    @Inject
    @ConfigProperty(name = "server.static.classpath.context")
    private val context: String? = null

    /**
     * Public page (does not require authentication).
     * If there is pre-emptive basic auth, it will run within a user context.
     *
     * @return web page with links to other resources
     */
    @get:Authenticated(optional = true)
    @get:Produces(MediaType.TEXT_HTML)
    @get:GET
    open val public: String
        get() = ("<html><head/><body>Hello World. This is a public page with no security "
                + "<a href=\"helloworld/admin\">Allowed for admin only</a><br>"
                + "<a href=\"helloworld/user\">Allowed for user only</a><br>"
                + "<a href=\"" + context + "/resource.html\">" + context + "/resource.html allowed for a logged in user</a><br>"
                + "you are logged in as: " + securityContext!!.user()
                + "</body></html>")

    /**
     * Page restricted to users in "admin" role.
     *
     * @param securityContext Helidon security context
     * @return web page with links to other resources
     */
    @GET
    @Path("/admin")
    @Produces(MediaType.TEXT_HTML)
    @Authenticated
    @Authorized
    @RolesAllowed("admin")
    open fun getAdmin(@Context securityContext: SecurityContext): String {
        return ("<html><head/><body>Hello World. You may want to check "
                + "<a href=\"" + context + "/resource.html\">" + context + "/resource.html</a><br>"
                + "you are logged in as: " + securityContext.user()
                + "</body></html>")
    }

    /**
     * Page restricted to users in "user" role.
     *
     * @param securityContext Helidon security context
     * @return web page with links to other resources
     */
    @GET
    @Path("/user")
    @Produces(MediaType.TEXT_HTML)
    @Authenticated
    @Authorized
    @RolesAllowed("user")
    open fun getUser(@Context securityContext: SecurityContext): String {
        return ("<html><head/><body>Hello World. You may want to check "
                + "<a href=\"" + context + "/resource.html\">" + context + "/resource.html</a><br>"
                + "you are logged in as: " + securityContext.user()
                + "</body></html>")
    }
}