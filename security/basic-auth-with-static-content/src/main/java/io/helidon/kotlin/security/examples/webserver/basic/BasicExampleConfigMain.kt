/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
import io.helidon.config.Config
import io.helidon.kotlin.security.examples.webserver.basic.BasicExampleUtil.startAndPrintEndpoints
import io.helidon.security.SecurityContext
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.webserver.*
import java.util.concurrent.TimeUnit

/**
 * Example using configuration based approach.
 */
object BasicExampleConfigMain {
    /**
     * Entry point, starts the server.
     * @param args not used
     */
    @JvmStatic
    fun main(args: Array<String>) {
        startAndPrintEndpoints(BasicExampleConfigMain::startServer)
    }

    @JvmStatic
    fun startServer(): WebServer {
        LogConfig.initClass()
        val config = Config.create()
        val routing = Routing.builder() // must be configured first, to protect endpoints
            .register(WebSecurity.create(config["security"]))
            .register("/static", StaticContentSupport.create("/WEB"))["/{*}", Handler { req: ServerRequest, res: ServerResponse ->
            val securityContext = req.context().get(SecurityContext::class.java)
            res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
            res.send("Hello, you are: \n" + securityContext
                .map { ctx: SecurityContext -> ctx.user().orElse(SecurityContext.ANONYMOUS).toString() }
                .orElse("Security context is null"))
        }]
            .build()
        return WebServer.builder()
            .config(config["server"])
            .routing(routing)
            .build()
            .start()
            .await(10, TimeUnit.SECONDS)
    }
}