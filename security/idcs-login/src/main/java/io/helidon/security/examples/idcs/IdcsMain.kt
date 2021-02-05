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
package io.helidon.security.examples.idcs

import io.helidon.common.http.MediaType
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.security.Security
import io.helidon.security.SecurityContext
import io.helidon.security.Subject
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.security.providers.oidc.OidcSupport
import io.helidon.webserver.*
import java.io.IOException
import java.util.logging.LogManager

/**
 * IDCS Login example main class using configuration .
 */
object IdcsMain {
    @Volatile
    lateinit var theServer: WebServer
        private set

    /**
     * Start the example.
     *
     * @param args ignored
     * @throws IOException if logging configuration fails
     */
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // load logging configuration
        LogManager.getLogManager().readConfiguration(IdcsMain::class.java.getResourceAsStream("/logging.properties"))
        val config = buildConfig()
        val security = Security.create(config["security"])
        val routing = Routing.builder()
                .register(WebSecurity.create(security, config["security"])) // IDCS requires a web resource for redirects
                .register(OidcSupport.create(config))["/rest/profile", Handler { req: ServerRequest, res: ServerResponse ->
            val securityContext = req.context().get(SecurityContext::class.java)
            res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
            res.send("Response from config based service, you are: \n" + securityContext
                    .flatMap { obj: SecurityContext -> obj.user() }
                    .map { obj: Subject -> obj.toString() }
                    .orElse("Security context is null"))
        }]
        theServer = WebServer.create(routing, config["server"])
        IdcsUtil.start(theServer)
    }

    private fun buildConfig(): Config {
        return Config.builder()
                .sources( // you can use this file to override the defaults built-in
                        ConfigSources.file(System.getProperty("user.home") + "/helidon/conf/examples.yaml").optional(),  // in jar file (see src/main/resources/application.yaml)
                        ConfigSources.classpath("application.yaml"))
                .build()
    }
}