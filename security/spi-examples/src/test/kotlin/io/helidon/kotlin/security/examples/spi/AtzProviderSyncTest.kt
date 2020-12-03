/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Unit test for [AtzProviderSync].
 */
class AtzProviderSyncTest {
    @Test
    fun testPublic() {
        val se = SecurityEnvironment.builder()
                .path("/public/some/path")
                .build()
        val ep = EndpointConfig.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        Mockito.`when`(request.env()).thenReturn(se)
        Mockito.`when`(request.endpointConfig()).thenReturn(ep)
        val provider = AtzProviderSync()
        val response = provider.syncAuthorize(request)
        MatcherAssert.assertThat(response.status(), CoreMatchers.`is`(SecurityResponse.SecurityStatus.SUCCESS))
    }

    @Test
    fun testAbstain() {
        val se = SecurityEnvironment.create()
        val ep = EndpointConfig.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        Mockito.`when`(request.env()).thenReturn(se)
        Mockito.`when`(request.endpointConfig()).thenReturn(ep)
        val provider = AtzProviderSync()
        val response = provider.syncAuthorize(request)
        MatcherAssert.assertThat(response.status(), CoreMatchers.`is`(SecurityResponse.SecurityStatus.ABSTAIN))
    }

    @Test
    fun testDenied() {
        val context = Mockito.mock(SecurityContext::class.java)
        Mockito.`when`(context.isAuthenticated).thenReturn(false)
        val se = SecurityEnvironment.builder()
                .path("/private/some/path")
                .build()
        val ep = EndpointConfig.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        Mockito.`when`(request.securityContext()).thenReturn(context)
        Mockito.`when`(request.env()).thenReturn(se)
        Mockito.`when`(request.endpointConfig()).thenReturn(ep)
        val provider = AtzProviderSync()
        val response = provider.syncAuthorize(request)
        MatcherAssert.assertThat(response.status(), CoreMatchers.`is`(SecurityResponse.SecurityStatus.FAILURE))
    }

    @Test
    fun testPermitted() {
        val context = Mockito.mock(SecurityContext::class.java)
        Mockito.`when`(context.isAuthenticated).thenReturn(true)
        val se = SecurityEnvironment.builder()
                .path("/private/some/path")
                .build()
        val ep = EndpointConfig.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        Mockito.`when`(request.securityContext()).thenReturn(context)
        Mockito.`when`(request.env()).thenReturn(se)
        Mockito.`when`(request.endpointConfig()).thenReturn(ep)
        val provider = AtzProviderSync()
        val response = provider.syncAuthorize(request)
        MatcherAssert.assertThat(response.status(), CoreMatchers.`is`(SecurityResponse.SecurityStatus.SUCCESS))
    }
}