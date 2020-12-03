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
package io.helidon.kotlin.examples.messaging.se

import io.helidon.config.Config
import io.helidon.messaging.Channel
import io.helidon.messaging.Emitter
import io.helidon.messaging.Messaging
import io.helidon.messaging.connectors.kafka.KafkaConnector
import io.helidon.webserver.*
import org.apache.kafka.common.serialization.StringSerializer

class SendingService internal constructor(config: Config) : Service {
    private val emitter: Emitter<String>
    private val messaging: Messaging

    /**
     * A service registers itself by updating the routing rules.
     *
     * @param rules the routing rules.
     */
    override fun update(rules: Routing.Rules) {
        // Listen for GET /example/send/{msg}
        // to send it thru messaging to Kafka
        rules["/send/{msg}", Handler { req: ServerRequest, res: ServerResponse ->
            val msg = req.path().param("msg")
            println("Emitting: $msg")
            emitter.send(msg)
            res.send()
        }]
    }

    /**
     * Gracefully terminate messaging.
     */
    fun shutdown() {
        messaging.stop()
    }

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
            .processor(toProcessor, toKafka) { payload: String -> payload.toUpperCase() }
            .connector(kafkaConnector)
            .build()
            .start()
    }
}