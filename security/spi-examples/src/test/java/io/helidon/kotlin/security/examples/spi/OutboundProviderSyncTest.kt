/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.spi

import io.helidon.security.*
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*
import org.hamcrest.CoreMatchers.`is` as Is
import org.mockito.Mockito.`when` as When

/**
 * Unit test for [OutboundProviderSync].
 */
class OutboundProviderSyncTest {
    @Test
    fun testAbstain() {
        val context = Mockito.mock(SecurityContext::class.java)
        When(context.user()).thenReturn(Optional.empty())
        When(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        When(request.securityContext()).thenReturn(context)
        When(request.env()).thenReturn(se)
        val ops = OutboundProviderSync()
        val response = ops.syncOutbound(request, SecurityEnvironment.create(), EndpointConfig.create())
        MatcherAssert.assertThat(response.status(), Is(SecurityResponse.SecurityStatus.ABSTAIN))
    }

    @Test
    fun testSuccess() {
        val username = "aUser"
        val subject = Subject.create(Principal.create(username))
        val context = Mockito.mock(SecurityContext::class.java)
        When(context.user()).thenReturn(Optional.of(subject))
        When(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        When(request.securityContext()).thenReturn(context)
        When(request.env()).thenReturn(se)
        val ops = OutboundProviderSync()
        val response = ops.syncOutbound(request, SecurityEnvironment.create(), EndpointConfig.create())
        MatcherAssert.assertThat(response.status(), Is(SecurityResponse.SecurityStatus.SUCCESS))
        MatcherAssert.assertThat(response.requestHeaders()["X-AUTH-USER"], Is(listOf(username)))
    }
}