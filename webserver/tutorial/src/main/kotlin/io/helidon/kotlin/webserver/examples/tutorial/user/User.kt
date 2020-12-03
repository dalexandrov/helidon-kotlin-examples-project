/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.tutorial.user

/**
 * Represents an immutable user.
 *
 *
 * [UserFilter] can be registered on Web Server [Routing] to provide valid [User]
 * instance on the request context.
 */
class User {
    private val isAuthenticated: Boolean
    val alias: String
    private val isAnonymous: Boolean

    /**
     * Creates new instance non-anonymous user.
     *
     * @param authenticated an authenticated is `true` if this user identity was validated
     * @param alias         an alias represents the name of the user which is visible for others
     */
    internal constructor(authenticated: Boolean, alias: String) {
        isAuthenticated = authenticated
        this.alias = alias
        isAnonymous = false
    }

    /**
     * Creates an unauthenticated user.
     *
     * @param alias an alias represents the name of the user which is visible for others
     */
    internal constructor(alias: String) : this(false, alias) {}

    /**
     * Creates an anonymous user instance.
     */
    private constructor() {
        isAnonymous = true
        isAuthenticated = false
        alias = "anonymous"
    }

    companion object {
        /**
         * Represents an anonymous user.
         */
        @JvmField
        val ANONYMOUS = User()
    }
}