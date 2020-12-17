/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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
 * This example shows how to read configuration from several files placed in selected directory.
 */
fun main() {
    /*
       Creates a config from files from specified directory.
       E.g. Kubernetes Secrets:
     */
    val secrets = Config.builder(ConfigSources.directory("config/sources/conf/secrets"))
        .disableEnvironmentVariablesSource()
        .disableSystemPropertiesSource()
        .build()
    val username = secrets["username"].asString().get()
    println("Username: $username")
    assert(username == "libor")
    val password = secrets["password"].asString().get()
    println("Password: $password")
    assert(password == "^ery\$ecretP&ssword")
}