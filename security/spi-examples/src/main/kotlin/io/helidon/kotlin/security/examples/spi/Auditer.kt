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

import io.helidon.security.spi.AuditProvider
import java.util.*
import java.util.function.Consumer

/**
 * Audit provider implementation.
 */
class Auditer : AuditProvider {
    // BEWARE this is a memory leak. Only for example purposes and for unit-tests
    private val messages: MutableList<AuditProvider.TracedAuditEvent> = LinkedList()
    fun getMessages(): List<AuditProvider.TracedAuditEvent> {
        return messages
    }

    override fun auditConsumer(): Consumer<AuditProvider.TracedAuditEvent> {
        return Consumer { event: AuditProvider.TracedAuditEvent ->
            // just dump to stdout and store in a list
            println(event.severity().toString() + ": " + event.tracingId() + ": " + event)
            messages.add(event)
        }
    }
}