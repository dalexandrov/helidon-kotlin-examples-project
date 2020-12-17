/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.examples.microprofile.security.oidc

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.microprofile.server.Server

/**
 * Main for MP.
 */

fun main() {
    val server = Server.builder()
        .config(buildConfig())
        .build()
        .start()
    println("http://localhost:" + server.port() + "/test")
}

private fun buildConfig(): Config {
    return Config.builder()
        .sources( // you can use this file to override the defaults that are built-in
            ConfigSources.file(System.getProperty("user.home") + "/helidon/conf/examples.yaml")
                .optional(),  // in jar file (see src/main/resources/application.yaml)
            ConfigSources.classpath("application.yaml")
        )
        .build()
}
