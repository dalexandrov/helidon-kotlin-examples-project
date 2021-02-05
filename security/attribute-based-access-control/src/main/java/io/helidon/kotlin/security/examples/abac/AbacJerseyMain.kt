/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.abac

import io.helidon.config.Config
import io.helidon.microprofile.server.Server

/**
 * Jersey example for Attribute based access control.
 */


fun main() {
    val config = Config.create()
    val server = Server.builder()
        .config(config)
        .port(8080)
        .build()
        .start()
    System.out.printf("Started server on localhost:%d%n", server.port())
    println()
    println("***********************")
    println("** Endpoints:        **")
    println("***********************")
    println("Using declarative authorization (ABAC):")
    System.out.printf("  http://localhost:%1\$d/rest/attributes%n", server.port())
    println("Using explicit authorization (ABAC):")
    System.out.printf("  http://localhost:%1\$d/rest/explicit%n", server.port())
}
