/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

import io.helidon.security.Security
import io.helidon.kotlin.security.examples.jersey.JerseyResources.OutboundSecurityResource
import io.helidon.security.integration.jersey.SecurityFeature
import io.helidon.security.providers.abac.AbacProvider
import io.helidon.security.providers.common.OutboundTarget
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider
import io.helidon.security.providers.httpauth.SecureUserStore
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import io.helidon.webserver.jersey.JerseySupport
import java.util.*
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

/**
 * Example of integration between Jersey and Security module using builders.
 */
object JerseyBuilderMain {
    private val USERS: MutableMap<String, SecureUserStore.User> = HashMap()

    @Volatile
    @JvmStatic
    lateinit var httpServer: WebServer
        private set

    private fun addUser(user: String, password: String, roles: List<String>) {
        USERS[user] = object : SecureUserStore.User {
            override fun login(): String {
                return user
            }

            private fun password(): CharArray {
                return password.toCharArray()
            }

            override fun isPasswordValid(password: CharArray): Boolean {
                return password().contentEquals(password)
            }

            override fun roles(): Collection<String> {
                return roles
            }
        }
    }

    private fun buildSecurity(): SecurityFeature {
        return SecurityFeature(
                Security.builder() // add the security provider to use
                        .addProvider(HttpBasicAuthProvider.builder()
                                .realm("helidon")
                                .userStore(users())
                                .addOutboundTarget(OutboundTarget.builder("propagate-all").build()))
                        .addProvider(AbacProvider.create())
                        .build())
    }

    private fun users(): SecureUserStore {
        return SecureUserStore { login: String -> Optional.ofNullable(USERS[login]) }
    }

    private fun buildJersey(): JerseySupport {
        return JerseySupport.builder() // register JAX-RS resource
                .register(JerseyResources.HelloWorldResource::class.java) // register JAX-RS resource demonstrating identity propagation
                .register(OutboundSecurityResource::class.java) // integrate security
                .register(buildSecurity())
                .register(ExceptionMapper<Exception> { exception ->
                    exception.printStackTrace()
                    Response.serverError().build()
                })
                .build()
    }

    /**
     * Main method of example. No arguments required, no configuration required.
     *
     * @param args empty is OK
     * @throws Throwable if server fails to start
     */
    @JvmStatic
    @Throws(Throwable::class)
    fun main(args: Array<String>?) {
        val routing = Routing.builder()
                .register("/rest", buildJersey())
        httpServer = JerseyUtil.startIt(routing, 8080)
        JerseyResources.setPort(httpServer.port())
    }

    init {
        addUser("jack", "password", listOf("user", "admin"))
        addUser("jill", "password", listOf("user"))
        addUser("john", "password", listOf())
    }
}