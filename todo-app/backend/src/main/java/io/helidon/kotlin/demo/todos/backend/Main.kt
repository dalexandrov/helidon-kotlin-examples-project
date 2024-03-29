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
package io.helidon.kotlin.demo.todos.backend

import config
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import server
import java.util.logging.LogManager

/**
 * Main class to start the service.
 */
class Main

fun main() {

    // load logging configuration
    LogManager.getLogManager().readConfiguration(
        Main::class.java.getResourceAsStream("/logging.properties")
    )
    val config = buildConfig()

    // as we need to use custom filter
    // we need to build Server with custom config
    val server = server {
        config(config)
    }
    server.start()
}

/**
 * Load the configuration from all sources.
 * @return the configuration root
 */
fun buildConfig(): Config {
    return config {
        sources(
            listOf(
                ConfigSources.environmentVariables(),  // expected on development machine
                // to override props for dev
                ConfigSources.file("dev.yaml").optional(),  // expected in k8s runtime
                // to configure testing/production values
                ConfigSources.file("prod.yaml").optional(),  // in jar file
                // (see src/main/resources/application.yaml)
                ConfigSources.classpath("application.yaml")
            )
        ) // support for passwords in configuration
        //                .addFilter(SecureConfigFilter.fromConfig())
    }
}
