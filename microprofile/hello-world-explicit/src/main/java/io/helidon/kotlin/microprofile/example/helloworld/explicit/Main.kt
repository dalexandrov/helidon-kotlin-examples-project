/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.microprofile.example.helloworld.explicit

import io.helidon.microprofile.server.Server

/**
 * Explicit example.
 */

fun main() {
    val server = Server.builder()
        .host("localhost") // use a random free port
        .port(0)
        .build()
    server.start()
    val endpoint = "http://" + server.host() + ":" + server.port()
    println("Started application on     $endpoint/helloworld")
    println("Metrics available on       $endpoint/metrics")
    println("Heatlh checks available on $endpoint/health")
}
