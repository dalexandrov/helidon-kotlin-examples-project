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

import io.helidon.security.Security
import io.helidon.security.SecurityEnvironment
import io.helidon.security.spi.ProviderSelectionPolicy
import org.junit.jupiter.api.Test

/**
 * Unit test for [ProviderSelector].
 */
class ProviderSelectorTest {
    @Test
    fun integrateIt() {
        val security = Security.builder()
                .providerSelectionPolicy { obj: ProviderSelectionPolicy.Providers -> ProviderSelector.create(obj) }
                .addProvider(AtnProviderSync())
                .addProvider(AtzProviderSync())
                .build()
        val context = security.createContext("unit-test")
        context.env(SecurityEnvironment.builder().path("/public/path"))
        val response = context.authorize()

        // if we reached here, the policy worked
    }
}