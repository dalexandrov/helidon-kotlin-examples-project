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
package io.helidon.kotlin.config.examples.mapping

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.config.objectmapping.Value
import to
import java.util.function.Supplier

/**
 * This example shows how to automatically deserialize configuration instance into POJO beans
 * using factory method.
 */

fun main() {
    val config = Config.create(ConfigSources.classpath("application.conf"))
    val appConfig = config["app"] // let config automatically deserialize the node to new AppConfig instance
        .to<AppConfigFact>()
        .get()
    println(appConfig)
    assert(appConfig.greeting == "Hello")
    assert(appConfig.pageSize == 20)
    assert(appConfig.basicRange.size == 2)
}

/**
 * POJO representing an application configuration.
 * Class is initialized from [Config] instance.
 * During deserialization [factory method][.create] is invoked.
 */
class AppConfigFact private constructor(val greeting: String, val pageSize: Int, val basicRange: List<Integer>) {
    override fun toString(): String {
        return """AppConfig:
    greeting  = $greeting
    pageSize  = $pageSize
    basicRange= $basicRange"""
    }

    /**
     * Supplier of default value for `basic-range` property, see [.create].
     */
    class DefaultBasicRangeSupplier : Supplier<List<Int>> {
        override fun get(): List<Int> {
            return listOf(-10, 10)
        }
    }

    companion object {
        /**
         * Creates new [AppConfigFact] instances.
         *
         *
         * [Value] is used to specify config keys
         * and default values in case configuration does not contain appropriate value.
         *
         * @param greeting   greeting
         * @param pageSize   page size
         * @param basicRange basic range
         * @return new instance of [AppConfigFact].
         */
        fun create(
            @Value(key = "greeting", withDefault = "Hi") greeting: String,
            @Value(key = "page-size", withDefault = "10") pageSize: Int,
            @Value(key = "basic-range", withDefaultSupplier = DefaultBasicRangeSupplier::class) basicRange: List<Integer>
        ): AppConfigFact {
            return AppConfigFact(greeting, pageSize, basicRange)
        }
    }
}