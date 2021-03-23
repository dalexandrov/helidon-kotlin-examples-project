/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates. All rights reserved.
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
 * using setters.
 */


fun main() {
    val config = Config.create(ConfigSources.classpath("application.conf"))
    val appConfig = config["app"] // let config automatically deserialize the node to new AppConfig instance
        .to<AppConfigDec>()
        .get()
    println(appConfig)
    assert(appConfig.greeting == "Hello")
    assert(appConfig.pageSize == 20)
    assert(appConfig.basicRange!!.size == 2)
    assert(appConfig.basicRange!![0] == -20)
    assert(appConfig.basicRange!![1] == 20)
}

/**
 * POJO representing an application configuration.
 * Class is initialized from [Config] instance.
 * During deserialization setter methods are invoked.
 */
class AppConfigDec {
    /**
     * Set greeting property.
     *
     *
     * POJO property and config key are same, no need to customize it.
     * [Value] is used just to specify default value
     * in case configuration does not contain appropriate value.
     *
     */
    @set:Value(withDefault = "Hi")
    var greeting: String? = null

    /**
     * Set a page size.
     *
     *
     * [Value] is used to specify correct config key and default value
     * in case configuration does not contain appropriate value.
     * Original string value is mapped to target int using appropriate
     * [ConfigMapper][io.helidon.config.ConfigMappers].
     *
     */
    @set:Value(key = "page-size", withDefault = "10")
    var pageSize = 0

    /**
     * Set a basic range.
     *
     *
     * [Value] is used to specify correct config key and default value supplier
     * in case configuration does not contain appropriate value.
     * Supplier already returns default value in target type of a property.
     *
     */
    @set:Value(key = "basic-range", withDefaultSupplier = DefaultBasicRangeSupplier::class)
    var basicRange: List<Int>? = null
    override fun toString(): String {
        return """AppConfig:
    greeting  = $greeting
    pageSize  = $pageSize
    basicRange= $basicRange"""
    }

    /**
     * Supplier of default value for [basic-range][.setBasicRange] property.
     */
    class DefaultBasicRangeSupplier : Supplier<List<Int>> {
        override fun get(): List<Int> {
            return listOf(-10, 10)
        }
    }
}
