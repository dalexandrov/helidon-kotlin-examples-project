/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.config.examples.changes

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.config.PollingStrategies
import java.time.Duration
import java.util.logging.Logger

/**
 * Example shows how to listen on Config node changes using simplified API, [Config.onChange].
 * The Function is invoked with new instance of Config.
 *
 *
 * The feature is based on using [io.helidon.config.spi.PollingStrategy] with
 * selected config source(s) to check for changes.
 */
class OnChangeExample {
    /**
     * Executes the example.
     */
    fun run() {
        val secrets = Config
                .builder(ConfigSources.directory("/config/changes/conf/secrets")
                        .pollingStrategy(PollingStrategies.regular(Duration.ofSeconds(5))))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build()
        logSecrets(secrets)

        // subscribe using simple onChange consumer -- could be a lambda as well
        secrets.onChange { secrets: Config -> logSecrets(secrets) }
    }

    companion object {
        private val LOGGER = Logger.getLogger(OnChangeExample::class.java.name)
        private fun logSecrets(secrets: Config) {
            LOGGER.info("Loaded secrets are u: " + secrets["username"].asString().get()
                    + ", p: " + secrets["password"].asString().get())
        }
    }
}