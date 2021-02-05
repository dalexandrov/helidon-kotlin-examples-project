/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.kotlin.security.examples.spi.AtnProviderSync.AtnObject
import io.helidon.kotlin.security.examples.spi.AtnProviderSync.AtnObject.Companion.from
import io.helidon.security.*
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*
import java.util.Map
import org.hamcrest.CoreMatchers.`is` as Is
import org.mockito.Mockito.`when` as When

/**
 * Unit test for [AtnProviderSync].
 */
class AtnProviderSyncTest {
    @Test
    fun testAbstain() {
        val context = Mockito.mock(SecurityContext::class.java)
        When(context.user()).thenReturn(Optional.empty())
        When(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val ep = EndpointConfig.create()
        val request = Mockito.mock(ProviderRequest::class.java)
        When(request.securityContext()).thenReturn(context)
        When(request.env()).thenReturn(se)
        When(request.endpointConfig()).thenReturn(ep)
        val provider = AtnProviderSync()
        val response = provider.syncAuthenticate(request)
        MatcherAssert.assertThat(response.status(), Is(SecurityResponse.SecurityStatus.ABSTAIN))
    }


    @Test
    fun testCustomObjectSuccess() {
        val obj = AtnObject()
        obj.size = SIZE
        obj.value = VALUE
        val context = Mockito.mock(SecurityContext::class.java)
        When(context.user()).thenReturn(Optional.empty())
        When(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val ep = EndpointConfig.builder()
                .customObject(AtnObject::class.java, obj)
                .build()
        val request = Mockito.mock(ProviderRequest::class.java)
        When(request.securityContext()).thenReturn(context)
        When(request.env()).thenReturn(se)
        When(request.endpointConfig()).thenReturn(ep)
        testSuccess(request)
    }

    @Test
    fun testConfigSuccess() {
        val config = Config.create(
                ConfigSources.create(Map.of("value", VALUE,
                        "size", SIZE.toString()))
        )
        val context = Mockito.mock(SecurityContext::class.java)
        When(context.user()).thenReturn(Optional.empty())
        When(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val ep = EndpointConfig.builder()
                .config("atn-object", config)
                .build()
        val request = Mockito.mock(ProviderRequest::class.java)
        When(request.securityContext()).thenReturn(context)
        When(request.env()).thenReturn(se)
        When(request.endpointConfig()).thenReturn(ep)
        testSuccess(request)
    }

    @Test
    fun testFailure() {
        val config = Config.create(
                ConfigSources.create(Map.of("atn-object.size", SIZE.toString()))
        )
        val context = Mockito.mock(SecurityContext::class.java)
        When(context.user()).thenReturn(Optional.empty())
        When(context.service()).thenReturn(Optional.empty())
        val se = SecurityEnvironment.create()
        val ep = EndpointConfig.builder()
                .config("atn-object", config)
                .build()
        val request = Mockito.mock(ProviderRequest::class.java)
        When(request.securityContext()).thenReturn(context)
        When(request.env()).thenReturn(se)
        When(request.endpointConfig()).thenReturn(ep)
        val provider = AtnProviderSync()
        val response = provider.syncAuthenticate(request)
        MatcherAssert.assertThat(response.status(), Is(SecurityResponse.SecurityStatus.FAILURE))
    }

    @Test
    fun integrationTest() {
        val security = Security.builder()
                .addProvider(AtnProviderSync())
                .build()

        // this part is usually done by container integration component
        // in Jersey you have access to security context through annotations
        // in Web server you have access to security context through context
        val context = security.createContext("unit-test")
        context.endpointConfig(EndpointConfig.builder()
                .customObject(AtnObject::class.java,
                        from(VALUE, SIZE)))
        val response = context.authenticate()
        validateResponse(response)
    }

    private fun validateResponse(response: AuthenticationResponse) {
        MatcherAssert.assertThat(response.status(), Is(SecurityResponse.SecurityStatus.SUCCESS))
        val maybeuser = response.user()
        maybeuser.ifPresentOrElse({ user: Subject ->
            MatcherAssert.assertThat(user.principal().id(), Is(VALUE))
            val roles = Security.getRoles(user)
            MatcherAssert.assertThat(roles.size, Is(1))
            MatcherAssert.assertThat(roles.iterator().next(), Is("role_$SIZE"))
        }) { Assertions.fail<Any>("User should have been returned") }
    }

    private fun testSuccess(request: ProviderRequest) {
        val provider = AtnProviderSync()
        val response = provider.syncAuthenticate(request)
        validateResponse(response)
    }

    companion object {
        private const val VALUE = "aValue"
        private const val SIZE = 16
    }
}