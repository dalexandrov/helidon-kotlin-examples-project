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

import asSingle
import io.helidon.kotlin.webserver.examples.websocket.MessageQueue.Companion.instance
import io.helidon.webserver.*

/**
 * Class MessageQueueResource.
 */
class MessageQueueService : Service {
    private val messageQueue = instance()
    override fun update(routingRules: Routing.Rules) {
        routingRules.post("/board", Handler { request: ServerRequest, response: ServerResponse -> handlePost(request, response) })
    }

    private fun handlePost(request: ServerRequest, response: ServerResponse) {
        request.content()
            .asSingle(String::class.java)
            .thenAccept {
                messageQueue.push(it!!)
                response.status(204).send()
            }
    }
}