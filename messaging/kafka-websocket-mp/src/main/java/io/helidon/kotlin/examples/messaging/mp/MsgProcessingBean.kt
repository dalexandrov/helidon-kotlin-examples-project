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

import io.helidon.common.reactive.Multi
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Outgoing
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams
import org.reactivestreams.FlowAdapters
import org.reactivestreams.Publisher
import java.util.concurrent.SubmissionPublisher
import javax.enterprise.context.ApplicationScoped

/**
 * Bean for message processing.
 */
@ApplicationScoped
open class MsgProcessingBean {
    private val emitter = SubmissionPublisher<String>()
    private val broadCaster = SubmissionPublisher<String>()

    /**
     * Create a publisher for the emitter.
     *
     * @return A Publisher from the emitter
     */
    @Outgoing("multiplyVariants")
    open fun preparePublisher(): Publisher<String> {
        // Create new publisher for emitting to by this::process
        return ReactiveStreams
            .fromPublisher(FlowAdapters.toPublisher(Multi.create(emitter)))
            .buildRs()
    }

    /**
     * Returns a builder for a processor that maps a string into three variants.
     *
     * @return ProcessorBuilder
     */
    @Incoming("multiplyVariants")
    @Outgoing("toKafka")
    open fun multiply(): ProcessorBuilder<String, Message<String>> {
        // Multiply to 3 variants of same message
        return ReactiveStreams.builder<String>()
            .flatMap { o: String ->
                ReactiveStreams.of( // upper case variant
                    o.toUpperCase(),  // repeat twice variant
                    o.repeat(2),  // reverse chars 'tnairav'
                    StringBuilder(o).reverse().toString()
                )
            }.map { payload: String? -> Message.of(payload) }
    }

    /**
     * Broadcasts an event.
     *
     * @param msg Message to broadcast
     */
    @Incoming("fromKafka")
    open fun broadcast(msg: String) {
        // Broadcast to all subscribers
        broadCaster.submit(msg)
    }

    /**
     * Subscribe new Multi to broadcasting publisher.
     *
     * @return new Multi subscribed to broadcaster
     */
    open fun subscribeMulti(): Multi<String> {
        return Multi.create(broadCaster)
    }

    /**
     * Emit a message.
     *
     * @param msg message to emit
     */
    open fun process(msg: String) {
        emitter.submit(msg)
    }
}