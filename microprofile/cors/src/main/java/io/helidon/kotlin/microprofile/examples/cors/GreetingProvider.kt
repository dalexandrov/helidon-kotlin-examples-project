/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.microprofile.examples.cors

import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.concurrent.atomic.AtomicReference
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Provider for greeting message.
 */
@ApplicationScoped
open class GreetingProvider @Inject constructor(@ConfigProperty(name = "app.greeting") message: String) {
    private val message = AtomicReference<String>()
    open fun getMessage(): String {
        return message.get()
    }

    open fun setMessage(message: String) {
        this.message.set(message)
    }

    /**
     * Create a new greeting provider, reading the message from configuration.
     *
     */
    init {
        this.message.set(message)
    }
}