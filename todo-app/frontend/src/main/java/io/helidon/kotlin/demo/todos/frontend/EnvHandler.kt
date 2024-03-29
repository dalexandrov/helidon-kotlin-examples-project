/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.demo.todos.frontend

import io.helidon.config.Config
import io.helidon.webserver.*

/**
 * Handles response to current environment name.
 */
class EnvHandler(config: Config) : Service {
    /**
     * The environment name.
     */
    @Volatile
    private var env: String
    override fun update(rules: Routing.Rules) {
        rules[Handler { _: ServerRequest?, res: ServerResponse -> res.send(env) }]
    }

    /**
     * Create a new `EnvHandler` instance.
     */
    init {
        val envConfig = config["env"]
        env = envConfig.asString().orElse("unknown")
        envConfig.onChange { config1: Config -> env = config1.asString().orElse("unknown") }
    }
}