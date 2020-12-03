/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.staticcontent

import io.helidon.webserver.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder
import javax.json.Json

/**
 * Counts access to the WEB service.
 */
class CounterService : Service {
    private val allAccessCounter = LongAdder()
    private val apiAccessCounter = AtomicInteger()
    override fun update(routingRules: Routing.Rules) {
        routingRules.any(Handler { request: ServerRequest, _: ServerResponse -> handleAny(request) })["/api/counter", Handler { _: ServerRequest, response: ServerResponse -> handleGet(response) }]
    }

    private fun handleAny(request: ServerRequest) {
        allAccessCounter.increment()
        request.next()
    }

    private fun handleGet(response: ServerResponse) {
        val apiAcc = apiAccessCounter.incrementAndGet()
        val result = JSON.createObjectBuilder()
                .add("all", allAccessCounter.toLong())
                .add("api", apiAcc)
                .build()
        response.send(result)
    }

    companion object {
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())
    }
}