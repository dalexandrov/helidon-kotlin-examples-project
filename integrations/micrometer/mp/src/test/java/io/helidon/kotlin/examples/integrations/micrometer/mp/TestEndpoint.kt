/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.integrations.micrometer.mp

import io.helidon.microprofile.tests.junit5.HelidonTest
import javax.ws.rs.client.WebTarget
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.json.JsonObject
import javax.ws.rs.core.MediaType

@HelidonTest
open class TestEndpoint {
    @Inject
    private lateinit var webTarget: WebTarget

    @Inject
    private lateinit var registry: MeterRegistry

    @Test
    open fun pingGreet() {
        var jsonObject = webTarget
            .path("/greet/Joe")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonObject::class.java)
        var responseString = jsonObject.getString("message")
        Assertions.assertEquals("Hello Joe!", responseString, "Response string")
        val counter = registry.counter(GreetResource.PERSONALIZED_GETS_COUNTER_NAME)
        val before = counter.count()
        jsonObject = webTarget
            .path("/greet/Jose")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(JsonObject::class.java)
        responseString = jsonObject.getString("message")
        Assertions.assertEquals("Hello Jose!", responseString, "Response string")
        val after = counter.count()
        Assertions.assertEquals(
            1.0,
            after - before,
            "Difference in personalized greeting counter between successive calls"
        )
    }
}