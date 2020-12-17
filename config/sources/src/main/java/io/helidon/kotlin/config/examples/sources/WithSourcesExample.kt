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
package io.helidon.kotlin.config.examples.sources

import io.helidon.config.Config
import io.helidon.config.ConfigSources

/**
 * This example shows how to merge the configuration from different sources.
 *
 * @see LoadSourcesExample
 */

fun main() {
    /*
       Creates a config source composed of following sources:
       - conf/dev.yaml - developer specific configuration, should not be placed in VCS;
       - conf/config.yaml - deployment dependent configuration, for example prod, stage, etc;
       - default.yaml - application default values, loaded form classpath;
       with a filter which convert values with keys ending with "level" to upper case
     */
    val config = Config
        .builder(
            ConfigSources.file("config/sources/conf/dev.yaml").optional(),
            ConfigSources.file("config/sources/conf/config.yaml").optional(),
            ConfigSources.classpath("default.yaml")
        )
        .addFilter { key: Config.Key, stringValue: String -> if (key.name() == "level") stringValue.toUpperCase() else stringValue }
        .build()

    // Environment type, from dev.yaml:
    val env = config["meta.env"].asString()
    env.ifPresent { e: String -> println("Environment: $e") }
    assert(env.get() == "DEV")

    // Default value (default.yaml): Config Sources Example
    val appName = config["app.name"].asString().get()
    println("Name: $appName")
    assert(appName == "Config Sources Example")

    // Page size, from config.yaml: 10
    val pageSize = config["app.page-size"].asInt().get()
    println("Page size: $pageSize")
    assert(pageSize == 10)

    // Applied filter (uppercase logging level), from dev.yaml: finest -> FINEST
    val level = config["component.audit.logging.level"].asString().get()
    println("Level: $level")
    assert(level == "FINE")
}
