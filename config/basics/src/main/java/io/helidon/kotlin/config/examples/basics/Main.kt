/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.config.examples.basics

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import to
import java.nio.file.Path

/**
 * Basics example.
 */


fun main() {
    val config = Config.create(ConfigSources.classpath("application.conf"))
    val pageSize = config["app.page-size"].asInt().get()
    val storageEnabled = config["app.storageEnabled"].asBoolean().orElse(false)
    val basicRange = config["app.basic-range"].asList(Int::class.java).get()
    val loggingOutputPath = config["logging.outputs.file.name"].to<Path>().get()
    println(pageSize)
    println(storageEnabled)
    println(basicRange)
    println(loggingOutputPath)

}