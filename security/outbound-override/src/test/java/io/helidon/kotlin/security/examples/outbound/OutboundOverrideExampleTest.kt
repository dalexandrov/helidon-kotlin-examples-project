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
 */
package io.helidon.kotlin.security.examples.outbound

import io.helidon.kotlin.security.examples.outbound.OutboundOverrideExample.clientPort
import io.helidon.kotlin.security.examples.outbound.OutboundOverrideExample.startClientService
import io.helidon.kotlin.security.examples.outbound.OutboundOverrideExample.startServingService
import io.helidon.security.Security
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider
import io.helidon.webclient.WebClient
import io.helidon.webclient.security.WebClientSecurity
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.hamcrest.Matchers.`is` as Is

/**
 * Test of security override example.
 */
class OutboundOverrideExampleTest {
    @Test
    fun testOverrideExample() {
        val value = webClient.get()
                .path("/override")
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, "jack")
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, "password")
                .request(String::class.java)
                .await()
        MatcherAssert.assertThat(value, Is("You are: jack, backend service returned: jill\n"))
    }

    @Test
    fun testPropagateExample() {
        val value = webClient.get()
                .path("/propagate")
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, "jack")
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, "password")
                .request(String::class.java)
                .await()
        MatcherAssert.assertThat(value, Is("You are: jack, backend service returned: jack\n"))
    }

    companion object {
        private lateinit var webClient: WebClient
        @BeforeAll
        @JvmStatic
        fun setup() {
            val first = startClientService(-1)
            val second = startServingService(-1)
            first.toCompletableFuture().join()
            second.toCompletableFuture().join()
            val security = Security.builder()
                    .addProvider(HttpBasicAuthProvider.builder().build())
                    .build()
            webClient = WebClient.builder()
                    .baseUri("http://localhost:" + clientPort())
                    .addService(WebClientSecurity.create(security))
                    .build()
        }
    }
}