/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.microprofile.example.websocket

import io.helidon.microprofile.server.Server
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.websocket.*
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity

/**
 * Class MessageBoardTest.
 */
class MessageBoardTest {
    private val messages = arrayOf("Whisky", "Tango", "Foxtrot")

    @Test

    @Throws(IOException::class, DeploymentException::class, InterruptedException::class)
    fun testBoard() {
//        // Post messages using REST resource
//        val restUri = URI.create("http://localhost:" + server!!.port() + "/rest")
//        for (message in messages) {
//            val res = restClient.target(restUri).request().post(Entity.text(message))
//            MatcherAssert.assertThat(res.status, CoreMatchers.`is`(204))
//            LOGGER.info("Posting message '$message'")
//        }
//
//        // Now connect to message board using WS and them back
//        val websocketUri = URI.create("ws://localhost:" + server!!.port() + "/websocket")
//        val messageLatch = CountDownLatch(messages.size)
//        val config = ClientEndpointConfig.Builder.create().build()
//        websocketClient.connectToServer(object : Endpoint() {
//            override fun onOpen(session: Session, EndpointConfig: EndpointConfig) {
//                try {
//                    // Set message handler to receive messages
//                    session.addMessageHandler(MessageHandler.Whole<String> { message ->
//                        LOGGER.info("Client OnMessage called '$message'")
//                        messageLatch.countDown()
//                        if (messageLatch.count == 0L) {
//                            try {
//                                session.close()
//                            } catch (e: IOException) {
//                                Assertions.fail<Any>("Unexpected exception $e")
//                            }
//                        }
//                    })
//
//                    // Send an initial message to start receiving
//                    session.basicRemote.sendText("SEND")
//                } catch (e: IOException) {
//                    Assertions.fail<Any>("Unexpected exception $e")
//                }
//            }
//
//            override fun onClose(session: Session, closeReason: CloseReason) {
//                LOGGER.info("Client OnClose called '$closeReason'")
//            }
//
//            override fun onError(session: Session, thr: Throwable) {
//                LOGGER.info("Client OnError called '$thr'")
//            }
//        }, config, websocketUri)
//
//        // Wait until all messages are received
//        messageLatch.await(1000000, TimeUnit.SECONDS)
    }

//    companion object {
//        private val LOGGER = Logger.getLogger(MessageBoardTest::class.java.name)
//        private val restClient = ClientBuilder.newClient()
//        private val websocketClient = ClientManager.createClient()
//        private var server: Server? = null
//
//        @BeforeAll
//        @JvmStatic
//        fun initClass() {
//            server = Server.create()
//            server!!.start()
//        }
//
//        @AfterAll
//        @JvmStatic
//        fun destroyClass() {
//            server!!.stop()
//        }
//    }
}