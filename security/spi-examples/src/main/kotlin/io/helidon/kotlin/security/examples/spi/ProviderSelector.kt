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

import io.helidon.security.NamedProvider
import io.helidon.security.spi.OutboundSecurityProvider
import io.helidon.security.spi.ProviderSelectionPolicy
import io.helidon.security.spi.SecurityProvider
import java.util.*
import java.util.function.Consumer

/**
 * Simple selector of providers, just chooses first, except for outbound, where it returns all.
 */
class ProviderSelector private constructor(private val providers: ProviderSelectionPolicy.Providers) : ProviderSelectionPolicy {
    private val outboundProviders: MutableList<OutboundSecurityProvider> = LinkedList()
    override fun <T : SecurityProvider> selectProvider(providerType: Class<T>): Optional<T> {
        val providers = providers.getProviders(providerType)
        return if (providers.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(providers[0].provider)
        }
    }

    override fun selectOutboundProviders(): List<OutboundSecurityProvider> {
        return outboundProviders
    }

    override fun <T : SecurityProvider?> selectProvider(providerType: Class<T>, requestedName: String): Optional<T> {
        return providers.getProviders(providerType)
                .stream()
                .filter { provider: NamedProvider<T> -> provider.name == requestedName }
                .findFirst()
                .map { obj: NamedProvider<T> -> obj.provider }
    }

    companion object {
        /**
         * This is the function to register with security.
         *
         * @param providers Providers from security
         * @return selector instance
         * @see io.helidon.security.Security.Builder.providerSelectionPolicy
         */
        @JvmStatic
        fun create(providers: ProviderSelectionPolicy.Providers): ProviderSelector {
            return ProviderSelector(providers)
        }
    }

    init {
        providers.getProviders(OutboundSecurityProvider::class.java)
                .forEach(Consumer { np: NamedProvider<OutboundSecurityProvider> -> outboundProviders.add(np.provider) })
    }
}