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
import io.helidon.messaging.Emitter
import io.helidon.messaging.Messaging
import io.helidon.messaging.connectors.kafka.KafkaConnector
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Locale

class SendingServiceRx(config: Config) {
    private val emitter: Emitter<String>
    private val messaging: Messaging

    init {
        val kafkaServer = config["app.kafka.bootstrap.servers"].asString().get()
        val topic = config["app.kafka.topic"].asString().get()

        // Prepare channel for connecting processor -> kafka connector with specific subscriber configuration,
        // channel -> connector mapping is automatic when using KafkaConnector.configBuilder()
        val toKafka = Channel.builder<String>()
            .subscriberConfig(
                KafkaConnector.configBuilder()
                    .bootstrapServers(kafkaServer)
                    .topic(topic)
                    .keySerializer(StringSerializer::class.java)
                    .valueSerializer(StringSerializer::class.java)
                    .build()
            ).build()

        // Prepare channel for connecting emitter -> processor
        val toProcessor = Channel.create<String>()

        // Prepare Kafka connector, can be used by any channel
        val kafkaConnector = KafkaConnector.create()

        // Prepare emitter for manual publishing to channel
        emitter = Emitter.create(toProcessor)
        messaging = Messaging.builder()
            .emitter(emitter) // Processor connect two channels together
            .processor(toProcessor, toKafka) { payload: String -> payload.uppercase(Locale.getDefault()) }
            .connector(kafkaConnector)
            .build()
            .start()
    }

    fun emitMessage(message: String) {
        emitter.send(message)
    }


    fun shutdown() {
        messaging.stop()
    }
}