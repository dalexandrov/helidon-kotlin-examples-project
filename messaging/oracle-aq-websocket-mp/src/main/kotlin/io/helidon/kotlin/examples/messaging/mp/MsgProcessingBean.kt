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

import io.helidon.common.reactive.BufferedEmittingPublisher
import io.helidon.common.reactive.Multi
import io.helidon.messaging.connectors.aq.AqMessage
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams
import org.reactivestreams.FlowAdapters
import org.reactivestreams.Publisher
import java.sql.SQLException
import java.util.List
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.SubmissionPublisher
import java.util.function.Supplier
import javax.enterprise.context.ApplicationScoped
import javax.jms.JMSException
import javax.jms.MapMessage

/**
 * Bean for message processing.
 */
@ApplicationScoped
open class MsgProcessingBean {
    private val emitter = BufferedEmittingPublisher.create<String>()
    private val broadCaster = SubmissionPublisher<String>()

    /**
     * Create a publisher for the emitter.
     *
     * @return A Publisher from the emitter
     */
    @Outgoing("to-queue-1")
    open fun toFirstQueue(): Publisher<String> {
        // Create new publisher for emitting to by this::process
        return ReactiveStreams
            .fromPublisher(FlowAdapters.toPublisher(emitter))
            .buildRs()
    }

    /**
     * Example of resending message from one queue to another and logging the payload to DB in the process.
     *
     * @param msg received message
     * @return message to be sent
     */
    @Incoming("from-queue-1")
    @Outgoing("to-queue-2") //Leave commit by ack to outgoing connector
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    open fun betweenQueues(msg: AqMessage<String>): CompletionStage<AqMessage<String>> {
        return CompletableFuture.supplyAsync {
            try {
                val statement = msg.dbConnection
                    .prepareStatement("INSERT INTO frank.message_log (message) VALUES (?)")
                statement.setString(1, msg.payload)
                statement.executeUpdate()
            } catch (e: SQLException) {
                //Gets caught by messaging engine and translated to onError signal
                throw RuntimeException("Error when saving message to log table.", e)
            }
            msg
        }
    }

    /**
     * Broadcasts an event.
     *
     * @param msg Message to broadcast
     */
    @Incoming("from-queue-2")
    open fun fromSecondQueue(msg: AqMessage<String>) {
        // Broadcast to all subscribers
        broadCaster.submit(msg.payload)
    }

    /**
     * Example of receiving a byte message.
     *
     * @param bytes received byte array
     */
    @Incoming("from-byte-queue")
    open fun fromByteQueue(bytes: ByteArray?) {
        broadCaster.submit(String(bytes!!))
    }

    /**
     * Example of receiving a map message.
     *
     * @param msg received JMS MapMessage
     * @throws JMSException when error arises during work with JMS message
     */
    @Incoming("from-map-queue")
    @Throws(JMSException::class)
    open fun fromMapQueue(msg: MapMessage) {
        val head = msg.getString("head")
        val body = msg.getBytes("body")
        val tail = msg.getString("tail")
        broadCaster.submit(java.lang.String.join(" ", List.of(head, String(body), tail)))
    }

    open fun subscribeMulti(): Multi<String> {
        return Multi.create(broadCaster).log()
    }

    open fun process(msg: String) {
        emitter.emit(msg)
    }
}