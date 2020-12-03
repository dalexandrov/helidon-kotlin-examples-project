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
 *
 */
package io.helidon.kotlin.examples.messaging.mp

import io.helidon.common.reactive.Single
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.websocket.*
import javax.websocket.server.ServerEndpoint

/**
 * Register all WebSocket connection as subscribers
 * of broadcasting [java.util.concurrent.SubmissionPublisher]
 * in the [MsgProcessingBean].
 *
 *
 * When connection is closed, cancel subscription and remove reference.
 */
@ServerEndpoint("/ws/messages")
open class WebSocketEndpoint {
    private val subscriberRegister: MutableMap<String, Single<Void>> = HashMap()

    @Inject
    private val msgProcessingBean: MsgProcessingBean? = null

    /**
     * On WebSocket session is opened.
     *
     * @param session        web socket session
     * @param endpointConfig endpoint config
     */
    @OnOpen
    open fun onOpen(session: Session, endpointConfig: EndpointConfig?) {
        println("New WebSocket client connected with session " + session.id)
        val single = msgProcessingBean!!.subscribeMulti() // Watch for errors coming from upstream
            .onError { throwable: Throwable? ->
                LOGGER.log(
                    Level.SEVERE,
                    "Upstream error!",
                    throwable
                )
            } // Send every item coming from upstream over web socket
            .forEach { s: String -> sendTextMessage(session, s) }

        //Save forEach single promise for later cancellation
        subscriberRegister[session.id] = single
    }

    /**
     * When WebSocket session is closed.
     *
     * @param session     web socket session
     * @param closeReason web socket close reason
     */
    @OnClose
    open fun onClose(session: Session, closeReason: CloseReason?) {
        LOGGER.info("Closing session " + session.id)
        // Properly unsubscribe from SubmissionPublisher
        Optional.ofNullable(subscriberRegister.remove(session.id))
            .ifPresent { obj: Single<Void> -> obj.cancel() }
    }

    private fun sendTextMessage(session: Session, msg: String) {
        try {
            session.basicRemote.sendText(msg)
        } catch (e: IOException) {
            LOGGER.log(Level.SEVERE, "Message sending over WebSocket failed", e)
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(WebSocketEndpoint::class.java.name)
    }
}