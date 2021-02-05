/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Logger
import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path

/**
 * Class MessageQueueResource.
 */
@Path("rest")
class MessageQueueResource {
    @Inject
    private lateinit var messageQueue: MessageQueue

    /**
     * Resource to push string into queue.
     *
     * @param s The string.
     */
    @POST
    @Consumes("text/plain")
    fun push(s: String) {
        LOGGER.info("push called '$s'")
        messageQueue.push(s)
    }

    companion object {
        private val LOGGER = Logger.getLogger(MessageQueueResource::class.java.name)
    }
}