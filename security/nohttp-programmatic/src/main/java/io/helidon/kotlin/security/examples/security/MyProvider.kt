/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.security

import io.helidon.security.*
import io.helidon.security.spi.AuthenticationProvider
import io.helidon.security.spi.AuthorizationProvider
import io.helidon.security.spi.OutboundSecurityProvider
import io.helidon.security.spi.SynchronousProvider
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Sample provider.
 */
internal class MyProvider : SynchronousProvider(), AuthenticationProvider, AuthorizationProvider, OutboundSecurityProvider {
    override fun syncAuthenticate(providerRequest: ProviderRequest): AuthenticationResponse {
        //get username and password
        val headers = providerRequest.env().headers().getOrDefault("authorization", listOf())
        if (headers.isEmpty()) {
            return AuthenticationResponse.failed("No authorization header")
        }
        val header = headers[0]
        if (header.toLowerCase().startsWith("basic ")) {
            val base64 = header.substring(6)
            val unamePwd = String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8)
            val index = unamePwd.indexOf(':')
            if (index > 0) {
                val name = unamePwd.substring(0, index)
                val pwd = unamePwd.substring(index + 1)
                if ("aUser" == name) {
                    //authenticate
                    val principal = Principal.create(name)
                    val roleGrant = Role.create("theRole")
                    val subject = Subject.builder()
                        .principal(principal)
                        .addGrant(roleGrant)
                        .addPrivateCredential(MyPrivateCreds::class.java, MyPrivateCreds(name, pwd.toCharArray()))
                        .build()
                    return AuthenticationResponse.success(subject)
                }
            }
        }
        return AuthenticationResponse.failed("User not found")
    }

    override fun syncAuthorize(providerRequest: ProviderRequest): AuthorizationResponse {
        return if ("CustomResourceType"
            == providerRequest.env().abacAttribute("resourceType").orElseThrow {
                IllegalArgumentException(
                    "Resource type is a required parameter"
                )
            }
        ) {
            //supported resource
            providerRequest.securityContext()
                .user()
                .map { obj: Subject -> obj.principal() }
                .map { obj: Principal -> obj.name }
                .map { anObject: String? -> "aUser" == anObject }
                .map { correct: Boolean ->
                    if (correct) {
                        return@map AuthorizationResponse.permit()
                    }
                    AuthorizationResponse.deny()
                }
                .orElse(AuthorizationResponse.deny())
        } else AuthorizationResponse.deny()
    }

    override fun syncOutbound(
        providerRequest: ProviderRequest,
        outboundEnv: SecurityEnvironment,
        outboundEndpointConfig: EndpointConfig
    ): OutboundSecurityResponse {
        return providerRequest.securityContext()
            .user()
            .flatMap { subject: Subject -> subject.privateCredential(MyPrivateCreds::class.java) }
            .map { myPrivateCreds: MyPrivateCreds ->
                OutboundSecurityResponse.builder()
                    .requestHeader("Authorization", authHeader(myPrivateCreds))
                    .build()
            }.orElse(OutboundSecurityResponse.abstain())
    }

    private fun authHeader(privCreds: MyPrivateCreds): String {
        val creds = privCreds.name + ":" + String(privCreds.password)
        return "basic " + Base64.getEncoder().encodeToString(creds.toByteArray(StandardCharsets.UTF_8))
    }

    private class MyPrivateCreds(val name: String, val password: CharArray) {
        override fun toString(): String {
            return "MyPrivateCreds: $name"
        }
    }
}