/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.service.wolt

import io.helidon.config.Config
import io.helidon.messaging.Channel
import io.helidon.messaging.Messaging
import io.helidon.messaging.connectors.kafka.KafkaConfigBuilder
import io.helidon.messaging.connectors.kafka.KafkaConnector
import org.apache.kafka.common.serialization.StringDeserializer
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Session

/**
 * Web socket endpoint.
 */
class WebSocketEndpoint : Endpoint() {
    private val messagingRegister: MutableMap<String, Messaging> = HashMap()
    private val config = Config.create()
    override fun onOpen(session: Session, endpointConfig: EndpointConfig) {
        println("Session " + session.id)
        val kafkaServer = config["app.kafka.bootstrap.servers"].asString().get()
        val topic = config["app.kafka.topic"].asString().get()

        // Prepare channel for connecting kafka connector with specific publisher configuration -> listener,
        // channel -> connector mapping is automatic when using KafkaConnector.configBuilder()
        val fromKafka = Channel.builder<String>()
            .name("from-kafka")
            .publisherConfig(
                KafkaConnector.configBuilder()
                    .bootstrapServers(kafkaServer)
                    .groupId("example-group-" + session.id)
                    .topic(topic)
                    .autoOffsetReset(KafkaConfigBuilder.AutoOffsetReset.LATEST)
                    .enableAutoCommit(true)
                    .keyDeserializer(StringDeserializer::class.java)
                    .valueDeserializer(StringDeserializer::class.java)
                    .build()
            )
            .build()

        // Prepare Kafka connector, can be used by any channel
        val kafkaConnector = KafkaConnector.create()
        val messaging = Messaging.builder()
            .connector(kafkaConnector)
            .listener(fromKafka) { payload: String ->
                println("Kafka says: $payload")
                // Send message received from Kafka over websocket
                sendTextMessage(session, payload)
            }
            .build()
            .start()

        //Save the messaging instance for proper shutdown
        // when websocket connection is terminated
        messagingRegister[session.id] = messaging
    }

    override fun onClose(session: Session, closeReason: CloseReason) {
        super.onClose(session, closeReason)
        LOGGER.info("Closing session " + session.id)
        // Properly stop messaging when websocket connection is terminated
        Optional.ofNullable(messagingRegister.remove(session.id))
            .ifPresent { obj: Messaging -> obj.stop() }
    }

    private fun sendTextMessage(session: Session, msg: String) {
        try {
            session.basicRemote.sendText(msg)
        } catch (e: IOException) {
            LOGGER.log(Level.SEVERE, "Message sending failed", e)
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(WebSocketEndpoint::class.qualifiedName)
    }
}