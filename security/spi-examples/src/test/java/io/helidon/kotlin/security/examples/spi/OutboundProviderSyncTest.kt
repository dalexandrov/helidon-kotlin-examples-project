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
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*

/**
 * Unit test for [OutboundProviderSync].
 */
class OutboundProviderSyncTest {
    @Test
    fun testAbstain() {
        val context = Mockito.mock(SecurityContext::class.java)
        Mockito.`when`(context.user()).thenReturn(Optional.empty())
        Mockito.`when`(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        Mockito.`when`(request.securityContext()).thenReturn(context)
        Mockito.`when`(request.env()).thenReturn(se)
        val ops = OutboundProviderSync()
        val response = ops.syncOutbound(request, SecurityEnvironment.create(), EndpointConfig.create())
        MatcherAssert.assertThat(response.status(), CoreMatchers.`is`(SecurityResponse.SecurityStatus.ABSTAIN))
    }

    @Test
    fun testSuccess() {
        val username = "aUser"
        val subject = Subject.create(Principal.create(username))
        val context = Mockito.mock(SecurityContext::class.java)
        Mockito.`when`(context.user()).thenReturn(Optional.of(subject))
        Mockito.`when`(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        Mockito.`when`(request.securityContext()).thenReturn(context)
        Mockito.`when`(request.env()).thenReturn(se)
        val ops = OutboundProviderSync()
        val response = ops.syncOutbound(request, SecurityEnvironment.create(), EndpointConfig.create())
        MatcherAssert.assertThat(response.status(), CoreMatchers.`is`(SecurityResponse.SecurityStatus.SUCCESS))
        MatcherAssert.assertThat(response.requestHeaders()["X-AUTH-USER"], CoreMatchers.`is`(listOf(username)))
    }
}