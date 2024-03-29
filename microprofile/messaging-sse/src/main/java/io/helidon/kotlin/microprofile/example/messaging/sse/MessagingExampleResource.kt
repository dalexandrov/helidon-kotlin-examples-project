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
 */
package io.helidon.kotlin.microprofile.example.messaging.sse

import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.sse.Sse
import javax.ws.rs.sse.SseEventSink

/**
 * Example resource with SSE.
 */
@Path("example")
@RequestScoped
open class MessagingExampleResource
/**
 * Constructor injection of field values.
 *
 * @param msgBean Messaging example bean
 */ @Inject constructor(private val msgBean: MsgProcessingBean) {
    /**
     * Process send.
     * @param msg message to process
     */
    @Path("/send/{msg}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    open fun getSend(@PathParam("msg") msg: String) {
        msgBean.process(msg)
    }

    /**
     * Consume event.
     *
     * @param eventSink sink
     * @param sse       event
     */
    @GET
    @Path("sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    open fun listenToEvents(@Context eventSink: SseEventSink?, @Context sse: Sse) {
        msgBean.addSink(eventSink, sse)
    }
}