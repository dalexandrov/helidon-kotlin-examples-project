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
package io.helidon.kotlin.webserver.examples.basics

import io.helidon.webserver.*

/**
 * Skeleton example of catalog resource use in [Main] class.
 */
class Catalog : Service {
    override fun update(rules: Routing.Rules) {
        rules["/", Handler { _: ServerRequest, response: ServerResponse -> list(response) }]["/{id}", Handler { req: ServerRequest, res: ServerResponse -> getSingle(res, req.path().param("id")) }]
    }

    private fun list(response: ServerResponse) {
        response.send("1, 2, 3, 4, 5")
    }

    private fun getSingle(response: ServerResponse, id: String) {
        response.send("Item: $id")
    }
}