/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Logger
import javax.websocket.*

/**
 * Class MessageBoardEndpoint.
 */
class MessageBoardEndpoint : Endpoint() {
    private val messageQueue = MessageQueue.instance()
    override fun onOpen(session: Session, endpointConfig: EndpointConfig) {
        session.addMessageHandler(MessageHandler.Whole<String> { message ->
            try {
                // Send all messages in the queue
                if (message == "SEND") {
                    while (!messageQueue.isEmpty) {
                        session.basicRemote.sendObject(messageQueue.pop())
                    }
                }
            } catch (e: Exception) {
                LOGGER.info(e.message)
            }
        })
    }

    override fun onClose(session: Session, closeReason: CloseReason) {
        super.onClose(session, closeReason)
    }

    override fun onError(session: Session, thr: Throwable) {
        super.onError(session, thr)
    }

    /**
     * Uppercase encoder.
     */
    class UppercaseEncoder : Encoder.Text<String> {
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