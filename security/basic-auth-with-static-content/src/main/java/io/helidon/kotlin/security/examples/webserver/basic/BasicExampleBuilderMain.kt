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
package io.helidon.kotlin.security.examples.webserver.basic

import io.helidon.common.LogConfig
import io.helidon.common.http.MediaType
import io.helidon.kotlin.security.examples.webserver.basic.BasicExampleUtil.startAndPrintEndpoints
import io.helidon.security.Security
import io.helidon.security.SecurityContext
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider
import io.helidon.security.providers.httpauth.SecureUserStore
import io.helidon.webserver.*
import routing
import security
import webServer
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Example using [io.helidon.common.Builder] approach instead of configuration based approach.
 */
object BasicExampleBuilderMain {
    // simple approach to user storage - for real world, use data store...
    private val USERS: MutableMap<String, MyUser> = HashMap()

    /**
     * Entry point, starts the server.
     *
     * @param args not used
     */
    @JvmStatic
    fun main(args: Array<String>) {
        startAndPrintEndpoints(BasicExampleBuilderMain::startServer)
    }

    @JvmStatic
    fun startServer(): WebServer {
        LogConfig.initClass()
        val routing = routing {   // must be configured first, to protect endpoints
            register(buildWebSecurity().securityDefaults(WebSecurity.authenticate()))
            any("/static[/{*}]", WebSecurity.rolesAllowed("user"))
            register(
                "/static",
                StaticContentSupport.create("/WEB")
            )
            get("/noRoles", WebSecurity.enforce())
            get("/user[/{*}]", WebSecurity.rolesAllowed("user"))
            get("/admin", WebSecurity.rolesAllowed("admin"))
            get("/deny", WebSecurity.rolesAllowed("deny").audit()) // roles allowed imply authn and authz
            any(
                "/noAuthn", WebSecurity.rolesAllowed("admin")
                    .authenticationOptional()
                    .audit()
            )
            get("/{*}", Handler { req: ServerRequest, res: ServerResponse ->
                val securityContext = req.context().get(SecurityContext::class.java)
                res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
                res.send("Hello, you are: \n" + securityContext
                    .map { ctx: SecurityContext -> ctx.user().orElse(SecurityContext.ANONYMOUS).toString() }
                    .orElse("Security context is null"))
            })
        }
        return webServer {
            routing(routing) // uncomment to use an explicit port
            //.port(8080)
        }
            .start()
            .await(10, TimeUnit.SECONDS)
    }

    private fun buildWebSecurity(): WebSecurity {
        val security = security {
            addAuthenticationProvider(
                HttpBasicAuthProvider.builder()
                    .realm("helidon")
                    .userStore(buildUserStore()),
                "http-basic-auth"
            )
        }
        return WebSecurity.create(security)
    }

    private fun buildUserStore(): SecureUserStore {
        return SecureUserStore { login: String -> Optional.ofNullable(USERS[login]) }
    }

    private class MyUser(private val login: String, private val password: CharArray, private val roles: Set<String>) :
        SecureUserStore.User {
        private fun password(): CharArray {
            return password
        }

        override fun isPasswordValid(password: CharArray): Boolean {
            return password().contentEquals(password)
        }

        override fun roles(): Set<String> {
            return roles
        }

        override fun login(): String {
            return login
        }
    }

    init {
        USERS["jack"] = MyUser("jack", "password".toCharArray(), mutableSetOf("user", "admin"))
        USERS["jill"] = MyUser("jill", "password".toCharArray(), mutableSetOf("user"))
        USERS["john"] = MyUser("john", "password".toCharArray(), mutableSetOf())
    }
}