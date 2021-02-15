/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.google

import config
import io.helidon.common.http.MediaType
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.security.SecurityContext
import io.helidon.security.Subject
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.webserver.*
import routing

/**
 * Google login button example main class using configuration.
 */
object GoogleConfigMain {
    @JvmStatic
    @Volatile
    lateinit var theServer: WebServer
        private set

    /**
     * Start the example.
     *
     * @param args ignored
     */
    @JvmStatic
    fun main(args: Array<String>) {
        start(GoogleUtil.PORT)
    }

    private fun buildConfig(): Config {
        return config {
            sources( // you can use this file to override the defaults built-in
                ConfigSources.file(System.getProperty("user.home") + "/helidon/conf/examples.yaml")
                    .optional(),  // in jar file (see src/main/resources/application.yaml)
                ConfigSources.classpath("application.yaml")
            )
        }
    }

    @JvmStatic
    fun start(port: Int): Int {
        val config = buildConfig()
        val routing =
            routing {  // helper method to load both security and web server security from configuration
                register(WebSecurity.create(config["security"]))
                get("/rest/profile", Handler { req: ServerRequest, res: ServerResponse ->
                    val securityContext = req.context().get(SecurityContext::class.java)
                    res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
                    res.send("Response from config based service, you are: \n" + securityContext
                        .flatMap { obj: SecurityContext -> obj.user() }
                        .map { obj: Subject -> obj.toString() }
                        .orElse("Security context is null"))
                })
                    .register(StaticContentSupport.create("/WEB"))
            }
        theServer = GoogleUtil.startIt(port, routing)
        return theServer.port()
    }
}