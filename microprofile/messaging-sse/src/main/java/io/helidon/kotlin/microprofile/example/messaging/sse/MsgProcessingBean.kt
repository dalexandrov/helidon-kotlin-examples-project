/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.microprofile.example.messaging.sse

import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams
import org.glassfish.jersey.media.sse.OutboundEvent
import org.reactivestreams.FlowAdapters
import org.reactivestreams.Publisher
import java.util.concurrent.SubmissionPublisher
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.sse.OutboundSseEvent
import javax.ws.rs.sse.Sse
import javax.ws.rs.sse.SseBroadcaster
import javax.ws.rs.sse.SseEventSink

/**
 * Bean for message processing.
 */
@ApplicationScoped
open class MsgProcessingBean {
    private val emitter = SubmissionPublisher<String>()
    private var sseBroadcaster: SseBroadcaster? = null

    /**
     * Create a publisher for the emitter.
     *
     * @return A Publisher from the emitter
     */
    @Outgoing("multiplyVariants")
    open fun preparePublisher(): Publisher<String> {
        // Create new publisher for emitting to by this::process
        return ReactiveStreams
                .fromPublisher(FlowAdapters.toPublisher(emitter))
                .buildRs()
    }

    /**
     * Returns a builder for a processor that maps a string into three variants.
     *
     * @return ProcessorBuilder
     */
    @Incoming("multiplyVariants")
    @Outgoing("wrapSseEvent")
    open fun multiply(): ProcessorBuilder<String, String> {
        // Multiply to 3 variants of same message
        return ReactiveStreams.builder<String>()
                .flatMap { o: String ->
                    ReactiveStreams.of( // upper case variant
                            o.toUpperCase(),  // repeat twice variant
                            o.repeat(2),  // reverse chars 'tnairav'
                            StringBuilder(o).reverse().toString())
                }
    }

    /**
     * Maps a message to an sse event.
     *
     * @param msg to wrap
     * @return an outbound SSE event
     */
    @Incoming("wrapSseEvent")
    @Outgoing("broadcast")
    open fun wrapSseEvent(msg: String?): OutboundSseEvent {
        // Map every message to sse event
        return OutboundEvent.Builder().data(msg).build()
    }

    /**
     * Broadcasts an event.
     *
     * @param sseEvent Event to broadcast
     */
    @Incoming("broadcast")
    open fun broadcast(sseEvent: OutboundSseEvent?) {
        // Broadcast to all sse sinks
        sseBroadcaster!!.broadcast(sseEvent)
    }

    /**
     * Consumes events.
     *
     * @param eventSink event sink
     * @param sse event
     */
    open fun addSink(eventSink: SseEventSink?, sse: Sse) {
        if (sseBroadcaster == null) {
            sseBroadcaster = sse.newBroadcaster()
        }
        sseBroadcaster!!.register(eventSink)
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