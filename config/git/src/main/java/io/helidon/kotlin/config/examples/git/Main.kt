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
package io.helidon.kotlin.config.examples.git

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.config.git.GitConfigSource
import java.net.URI

/**
 * Git source example.
 *
 *
 * This example expects:
 *
 *  1. a Git repository `helidonrobot/test-config` which contains:
 *
 *  1. the branch `test` containing `application.conf` which sets
 * `greeting` to `hello`,
 *  1. the branch `main` containing the file `application.conf`
 * which sets the property `greeting` to any value other than
 * `hello`,
 *  1. optionally, any other branch in which `application.conf` sets
 * `greeting` to `hello`.
 *
 *  1. the environment variable `ENVIRONMENT_NAME` set to:
 *
 *  1. `test`, or
 *  1. the name of the optional additional branch described above.
 *
 *
 */
private const val ENVIRONMENT_NAME_PROPERTY = "ENVIRONMENT_NAME"

/**
 * Executes the example.
 */
fun main() {

    // we expect a name of the current environment in envvar ENVIRONMENT_NAME
    // in this example we just set envvar in maven plugin 'exec', but can be set in k8s pod via ConfigMap
    val env = Config.create(ConfigSources.environmentVariables())
    val branch = env[ENVIRONMENT_NAME_PROPERTY].asString().orElse("master")
    println("Loading from branch $branch")
    val config = Config.create(
        GitConfigSource.builder()
            .path("application.conf")
            .uri(URI.create("https://github.com/helidonrobot/test-config.git"))
            .branch(branch)
            .build()
    )
    println("Greeting is " + config["greeting"].asString().get())
    assert(config["greeting"].asString().get() == "hello")
}