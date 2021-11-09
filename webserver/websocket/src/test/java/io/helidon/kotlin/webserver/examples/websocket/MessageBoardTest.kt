/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.websocket

import io.helidon.common.http.Http
import io.helidon.webclient.WebClient
import io.helidon.webserver.WebServer
import org.glassfish.tyrus.client.ClientManager
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import java.io.IOException
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.websocket.*
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * Class MessageBoardTest.
 */
class MessageBoardTest {
    private val messages = arrayOf("Whisky", "Tango", "Foxtrot")

    @Throws(IOException::class, DeploymentException::class, InterruptedException::class, ExecutionException::class)
    fun testBoard() {
        // Post messages using REST resource
        val restUri = URI.create("http://localhost:" + server.port() + "/rest/board")
        for (message in messages) {
            restClient.post()
                .uri(restUri)
                .submit(message)
                .thenAccept { MatcherAssert.assertThat(it.status(), Is(Http.Status.NO_CONTENT_204)) }
                .toCompletableFuture()
                .get()
            LOGGER.info("Posting message '$message'")
        }

        // Now connect to message board using WS and them back
        val websocketUri = URI.create("ws://localhost:" + server.port() + "/websocket/board")
        val messageLatch = CountDownLatch(messages.size)
        val config = ClientEndpointConfig.Builder.create().build()
        websocketClient.connectToServer(object : Endpoint() {
            override fun onOpen(session: Session, EndpointConfig: EndpointConfig) {
                try {
                    // Set message handler to receive messages
                    session.addMessageHandler(MessageHandler.Whole<String> { message ->
                        LOGGER.info("Client OnMessage called '$message'")
                        messageLatch.countDown()
                        if (messageLatch.count == 0L) {
                            try {
                                session.close()
                            } catch (e: IOException) {
                                Assertions.fail<Any>("Unexpected exception $e")
                            }
                        }
                    })

                    // Send an initial message to start receiving
                    session.basicRemote.sendText("SEND")
                } catch (e: IOException) {
                    Assertions.fail<Any>("Unexpected exception $e")
                }
            }

            override fun onClose(session: Session, closeReason: CloseReason) {
                LOGGER.info("Client OnClose called '$closeReason'")
            }

            override fun onError(session: Session, thr: Throwable) {
                LOGGER.info("Client OnError called '$thr'")
            }
        }, config, websocketUri)

        // Wait until all messages are received
        messageLatch.await(1000000, TimeUnit.SECONDS)
    }

    companion object {
        private val LOGGER = Logger.getLogger(MessageBoardTest::class.java.name)
        private val restClient = WebClient.create()
        private val websocketClient = ClientManager.createClient()
        private lateinit var server: WebServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            server = startWebServer()
        }

        @AfterAll
        @JvmStatic
        fun destroyClass() {
            server.shutdown()
        }
    }
}