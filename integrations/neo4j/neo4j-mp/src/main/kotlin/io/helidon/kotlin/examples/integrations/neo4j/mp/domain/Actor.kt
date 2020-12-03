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
package io.helidon.kotlin.examples.integrations.neo4j.mp.domain

import java.util.*

/**
 * The Actor class
 */
class Actor(private val name: String, roles: List<String>?) {
    private val roles: List<String>

    /**
     * Constructor.
     * @param name
     * @param roles
     */
    init {
        this.roles = ArrayList(roles)
    }
}