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
package io.helidon.kotlin.security.examples.signatures

import io.helidon.common.http.MediaType
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.security.SecurityContext
import io.helidon.security.Subject
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.webserver.*

/**
 * Example of authentication of service with http signatures, using configuration file as much as possible.
 */
object SignatureExampleConfigMain {
    // used from unit tests
    @JvmStatic
    lateinit var service1Server: WebServer
        private set
    @JvmStatic
    lateinit var service2Server: WebServer
        private set

    /**
     * Starts this example.
     *
     * @param args ignored
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // to allow us to set host header explicitly
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true")

        // start service 2 first, as it is required by service 1
        service2Server = SignatureExampleUtil.startServer(routing2(), 9080)
        service1Server = SignatureExampleUtil.startServer(routing1(), 8080)
        println("Signature example: from configuration")
        println()
        println("Users:")
        println("jack/password in roles: user, admin")
        println("jill/password in roles: user")
        println("john/password in no roles")
        println()
        println("***********************")
        println("** Endpoints:        **")
        println("***********************")
        println("Basic authentication, user role required, will use symmetric signatures for outbound:")
        System.out.printf("  http://localhost:%1\$d/service1%n", service1Server.port())
        println("Basic authentication, user role required, will use asymmetric signatures for outbound:")
        System.out.printf("  http://localhost:%1\$d/service1-rsa%n", service1Server.port())
        println()
    }

    private fun routing2(): Routing {
        val config = config("service2.yaml")
        // build routing (security is loaded from config)
        return Routing.builder() // helper method to load both security and web server security from configuration
                .register(WebSecurity.create(config["security"]))["/{*}", Handler { req: ServerRequest, res: ServerResponse ->
            val securityContext = req.context().get(SecurityContext::class.java)
            res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
            res.send("Response from service2, you are: \n" + securityContext
                    .flatMap { obj: SecurityContext -> obj.user() }
                    .map { obj: Subject -> obj.toString() }
                    .orElse("Security context is null") + ", service: " + securityContext
                    .flatMap { obj: SecurityContext -> obj.service() }
                    .map { obj: Subject -> obj.toString() })
        }]
                .build()
    }

    private fun routing1(): Routing {
        val config = config("service1.yaml")

        // build routing (security is loaded from config)
        return Routing.builder() // helper method to load both security and web server security from configuration
                .register(WebSecurity.create(config["security"]))["/service1", Handler { req: ServerRequest?, res: ServerResponse? -> SignatureExampleUtil.processService1Request(req, res, "/service2", service2Server!!.port()) }]["/service1-rsa", Handler { req: ServerRequest?, res: ServerResponse? -> SignatureExampleUtil.processService1Request(req, res, "/service2-rsa", service2Server!!.port()) }]
                .build()
    }

    private fun config(confFile: String): Config {
        // load configuration
        return Config.builder()
                .sources(ConfigSources.classpath(confFile))
                .build()
    }
}