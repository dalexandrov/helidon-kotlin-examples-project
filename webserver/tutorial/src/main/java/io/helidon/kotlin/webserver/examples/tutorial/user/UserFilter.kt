/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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

import io.helidon.webserver.Handler
import io.helidon.webserver.ServerRequest
import io.helidon.webserver.ServerResponse

/**
 * If used as a [Routing] [Handler] then assign valid [User] instance on the request
 * [context][io.helidon.common.context.Context].
 */
class UserFilter : Handler {
    override fun accept(req: ServerRequest, res: ServerResponse) {
        // Register as a supplier. Thanks to it, user instance is resolved ONLY if it is requested in downstream handlers.
        req.context().supply(User::class.java
        ) {
            req.headers()
                    .cookies()
                    .first("Unauthenticated-User-Alias")
                    .map { alias: String? -> User(alias!!) }
                    .orElse(User.ANONYMOUS)
        }
        req.next()
    }
}