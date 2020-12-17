/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

import io.helidon.config.Config
import io.helidon.security.Security
import io.helidon.kotlin.security.examples.jersey.JerseyResources.HelloWorldProgrammaticResource
import io.helidon.kotlin.security.examples.jersey.JerseyResources.OutboundSecurityResource
import io.helidon.security.integration.jersey.SecurityFeature
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import io.helidon.webserver.jersey.JerseySupport
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

/**
 * Example of integration between Jersey and Security module using config,
 * yet no container security - all security enforcement is by hand.
 */
object JerseyProgrammaticMain {
    @Volatile
    @JvmStatic
    lateinit var httpServer: WebServer
        private set

    private fun buildSecurity(): SecurityFeature {
        return SecurityFeature(Security.create(Config.create()["security"]))
    }

    private fun buildJersey(): JerseySupport {
        return JerseySupport.builder() // register JAX-RS resource
                .register(HelloWorldProgrammaticResource::class.java) // register JAX-RS resource demonstrating identity propagation (propagation
                // itself is programmatic only, this resource uses annotation to protect itself
                .register(OutboundSecurityResource::class.java) // integrate security
                .register(buildSecurity())
                .register(ExceptionMapper<Exception> { exception ->
                    if (exception is WebApplicationException) {
                        return@ExceptionMapper exception.response
                    }
                    exception.printStackTrace()
                    Response.serverError().build()
                })
                .build()
    }

    /**
     * Main method of example. No arguments required, no configuration required.
     *
     * @param args empty is OK
     */
    @JvmStatic
    fun main(args: Array<String>?) {
        val routing = Routing.builder()
                .register("/rest", buildJersey())
        httpServer = JerseyUtil.startIt(routing, 8080)
        JerseyResources.setPort(httpServer.port())
    }
}