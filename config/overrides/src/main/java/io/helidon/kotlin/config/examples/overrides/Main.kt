/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.config.examples.overrides

import config
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.config.OverrideSources
import io.helidon.config.PollingStrategies
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Overrides example.
 *
 *
 * Shows the Overrides feature where values from config sources might be overridden by override source.
 *
 *
 * In this example, `application.yaml` is meant to be default application configuration distributed with an app, containing
 * a wildcard configuration nodes representing the defaults for every environment and pod as well as a default definition of
 * these values. The source `conf/priority-config.yaml` is a higher priority configuration source which can be in a real
 * app dynamically changed (i.e. `Kubernetes ConfigMap` mapped as the file) and contains the current `env` and `pod` values (`prod` and `abcdef` in this example) and higher priority default configuration. So far the current
 * configuration looks like this:
 * <pre>
 * prod:
 * abcdef:
 * logging:
 * level: ERROR
 * app:
 * greeting:  Ahoy
 * page-size: 42
 * basic-range:
 * - -20
 * -  20
</pre> *
 * But the override source just overrides values for environment: `prod` and pod: `abcdef` (it is the first
 * overriding rule found) and value for key `prod.abcdef.logging.level = FINEST`. For completeness, we would say that the
 * other pods in `prod` environment has overridden config value `prod.*.logging.level` to `WARNING` and all
 * pods
 * `test.*.logging.level` to `FINE`.
 */
fun main() {
    val config = config {
        sources(
            ConfigSources.file("config/overrides/conf/priority-config.yaml")
                .pollingStrategy(PollingStrategies.regular(Duration.ofSeconds(1))),
            ConfigSources.classpath("application.yaml")
        ) // specify overrides source
        overrides(
            OverrideSources.file("config/overrides/conf/overrides.properties")
                .pollingStrategy(PollingStrategies.regular(Duration.ofSeconds(1)))
        )
    }

    // Resolve current runtime context
    val env = config["env"].asString().get()
    val pod = config["pod"].asString().get()

    // get logging config for the current runtime
    val loggingConfig = config[env][pod]["logging"]

    // initialize logging from config
    initLogging(loggingConfig)

    // react on changes of logging configuration
    loggingConfig.onChange { obj: Config? -> obj?.let { initLogging(it) } }
    TimeUnit.MINUTES.sleep(1)
}

/**
 * Initialize logging from config.
 */
private fun initLogging(loggingConfig: Config) {
    val level = loggingConfig["level"].asString().orElse("WARNING")
    //e.g. initialize logging using configured level...
    println("Set logging level to $level.")
}
