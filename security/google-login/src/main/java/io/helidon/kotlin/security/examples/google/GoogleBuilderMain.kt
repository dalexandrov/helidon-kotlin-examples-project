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

import io.helidon.common.http.MediaType
import io.helidon.security.Security
import io.helidon.security.SecurityContext
import io.helidon.security.Subject
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.security.providers.google.login.GoogleTokenProvider
import io.helidon.webserver.*

/**
 * Google login button example main class using builders.
 */
object GoogleBuilderMain {
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

    @JvmStatic
    fun start(port: Int): Int {
        val security = Security.builder()
                .addProvider(GoogleTokenProvider.builder()
                        .clientId("your-client-id.apps.googleusercontent.com"))
                .build()
        val ws = WebSecurity.create(security)
        val routing = Routing.builder()
                .register(ws)["/rest/profile", WebSecurity.authenticate(), Handler { req: ServerRequest, res: ServerResponse ->
            val securityContext = req.context().get(SecurityContext::class.java)
            res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
            res.send("Response from builder based service, you are: \n" + securityContext
                    .flatMap { obj: SecurityContext -> obj.user() }
                    .map { obj: Subject -> obj.toString() }
                    .orElse("Security context is null"))
            req.next()
        }]
                .register(StaticContentSupport.create("/WEB"))
        theServer = GoogleUtil.startIt(port, routing)
        return theServer.port()
    }
}