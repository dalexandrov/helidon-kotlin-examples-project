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
package io.helidon.kotlin.webserver.examples.mtls

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.config.spi.ConfigSource
import io.helidon.kotlin.webserver.examples.mtls.ClientBuilderMain.createWebClient
import io.helidon.kotlin.webserver.examples.mtls.ServerBuilderMain.startServer
import io.helidon.kotlin.webserver.examples.mtls.ServerConfigMain.startServer
import io.helidon.webclient.WebClient
import io.helidon.webserver.WebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

/**
 * Test of mutual TLS example.
 */
class MutualTlsExampleTest {

    private lateinit var webServer: WebServer
    @AfterEach
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun killServer() {
        webServer.shutdown()
                .toCompletableFuture()[10, TimeUnit.SECONDS]
    }

    @Test
    @Throws(InterruptedException::class)
    fun testConfigAccessSuccessful() {
        val config = Config.just(Supplier<ConfigSource> { ConfigSources.classpath("application-test.yaml").build() })
        waitForServerToStart(startServer(config["server"]))
        val webClient = WebClient.create(config["client"])
        MatcherAssert.assertThat(ClientConfigMain.callUnsecured(webClient, webServer.port()), CoreMatchers.`is`("Hello world unsecured!"))
        MatcherAssert.assertThat(ClientConfigMain.callSecured(webClient, webServer.port("secured")), CoreMatchers.`is`("Hello Helidon-client!"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBuilderAccessSuccessful() {
        waitForServerToStart(startServer(-1, -1))
        val webClient = createWebClient()
        MatcherAssert.assertThat(ClientBuilderMain.callUnsecured(webClient, webServer.port()), CoreMatchers.`is`("Hello world unsecured!"))
        MatcherAssert.assertThat(ClientBuilderMain.callSecured(webClient, webServer.port("secured")), CoreMatchers.`is`("Hello Helidon-client!"))
    }

    @Throws(InterruptedException::class)
    private fun waitForServerToStart(webServer: WebServer) {
        this.webServer = webServer
        val timeout: Long = 2000 // 2 seconds should be enough to start the server
        val now = System.currentTimeMillis()
        while (!webServer.isRunning) {
            Thread.sleep(100)
            if (System.currentTimeMillis() - now > timeout) {
                Assertions.fail<Any>("Failed to start webserver")
            }
        }
    }
}