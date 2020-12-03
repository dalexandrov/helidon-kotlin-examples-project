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
import io.helidon.security.spi.OutboundSecurityProvider
import io.helidon.security.spi.SynchronousProvider
import java.util.List
import java.util.Map

/**
 * Example of a simplistic outbound security provider.
 */
class OutboundProviderSync : SynchronousProvider(), OutboundSecurityProvider {
    public override fun syncOutbound(providerRequest: ProviderRequest,
                                     outboundEnv: SecurityEnvironment,
                                     outboundEndpointConfig: EndpointConfig): OutboundSecurityResponse {

        // let's just add current user's id as a custom header, otherwise do nothing
        return providerRequest.securityContext()
                .user()
                .map { obj: Subject -> obj.principal() }
                .map { obj: Principal -> obj.name }
                .map { name: String ->
                    OutboundSecurityResponse
                            .withHeaders(Map.of("X-AUTH-USER", List.of(name)))
                }
                .orElse(OutboundSecurityResponse.abstain())
    }
}