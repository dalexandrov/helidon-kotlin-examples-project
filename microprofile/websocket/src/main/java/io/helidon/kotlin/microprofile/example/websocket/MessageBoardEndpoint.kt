/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException
import java.util.logging.Logger
import javax.inject.Inject
import javax.websocket.*
import javax.websocket.server.ServerEndpoint

/**
 * Class MessageBoardEndpoint.
 */
@ServerEndpoint(value = "/websocket", encoders = [MessageBoardEndpoint.UppercaseEncoder::class])
open class MessageBoardEndpoint {
    @Inject
    private lateinit var messageQueue: MessageQueue

    /**
     * OnOpen call.
     *
     * @param session The websocket session.
     * @throws IOException If error occurs.
     */
    @OnOpen
    @Throws(IOException::class)
    open fun onOpen(session: Session?) {
        LOGGER.info("OnOpen called")
    }

    /**
     * OnMessage call.
     *
     * @param session The websocket session.
     * @param message The message received.
     * @throws Exception If error occurs.
     */
    @OnMessage
    @Throws(Exception::class)
    open fun onMessage(session: Session, message: String) {
        LOGGER.info("OnMessage called '$message'")

        // Send all messages in the queue
        if (message == "SEND") {
            while (!messageQueue.isEmpty) {
                session.basicRemote.sendObject(messageQueue.pop())
            }
        }
    }

    /**
     * OnError call.
     *
     * @param t The throwable.
     */
    @OnError
    open fun onError(t: Throwable?) {
        LOGGER.info("OnError called")
    }

    /**
     * OnError call.
     *
     * @param session The websocket session.
     */
    @OnClose
    open fun onClose(session: Session?) {
        LOGGER.info("OnClose called")
    }

    /**
     * Uppercase encoder.
     */
    open class UppercaseEncoder : Encoder.Text<String> {
        override fun encode(s: String): String {
            LOGGER.info("UppercaseEncoder encode called")
            return s.toUpperCase()
        }

        override fun init(config: EndpointConfig) {}
        override fun destroy() {}
    }

    companion object {
        private val LOGGER = Logger.getLogger(MessageBoardEndpoint::class.java.name)
    }
}