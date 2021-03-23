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
import io.helidon.kotlin.config.examples.mapping.AppConfig.Builder
import to
import java.util.function.Supplier

/**
 * This example shows how to automatically deserialize configuration instance into POJO beans
 * using Builder pattern.
 */

fun main() {
    val config = Config.create(ConfigSources.classpath("application.conf"))
    val appConfig = config["app"] // let config automatically deserialize the node to new AppConfig instance
        // note that this requires additional dependency - config-beans
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
 * During deserialization [.builder] builder method} is invoked
 * and [Builder] is used to initialize properties from configuration.
 */
class AppConfig private constructor(val greeting: String?, val pageSize: Int, val basicRange: List<Int>?) {
    override fun toString(): String {
        return """AppConfig:
    greeting  = $greeting
    pageSize  = $pageSize
    basicRange= $basicRange"""
    }

    /**
     * [AppConfigDec] Builder used to be initialized from configuration.
     */
    class Builder {
        private var greeting: String? = null
        private var pageSize = 0
        private var basicRange: List<Int>? = null

        /**
         * Set greeting property.
         *
         *
         * POJO property and config key are same, no need to customize it.
         * [Value] is used just to specify default value
         * in case configuration does not contain appropriate value.
         *
         * @param greeting greeting value
         */
        @Value(withDefault = "Hi")
        fun setGreeting(greeting: String?) {
            this.greeting = greeting
        }

        /**
         * Set a page size.
         *
         *
         * [Value] is used to specify correct config key and default value
         * in case configuration does not contain appropriate value.
         * Original string value is mapped to target int using appropriate
         * [ConfigMapper][io.helidon.config.ConfigMappers].
         *
         * @param pageSize page size
         */
        @Value(key = "page-size", withDefault = "10")
        fun setPageSize(pageSize: Int) {
            this.pageSize = pageSize
        }

        /**
         * Set a basic range.
         *
         *
         * [Value] is used to specify correct config key and default value supplier
         * in case configuration does not contain appropriate value.
         * Supplier already returns default value in target type of a property.
         *
         * @param basicRange basic range
         */
        @Value(key = "basic-range", withDefaultSupplier = DefaultBasicRangeSupplier::class)
        fun setBasicRange(basicRange: List<Int>?) {
            this.basicRange = basicRange
        }

        /**
         * Creates new instance of [AppConfigDec] using values provided by configuration.
         *
         * @return new instance of [AppConfigDec].
         */
        fun build(): AppConfig {
            return AppConfig(greeting, pageSize, basicRange)
        }
    }

    /**
     * Supplier of default value for [basic-range][Builder.setBasicRange] property.
     */
    class DefaultBasicRangeSupplier : Supplier<List<Int>> {
        override fun get(): List<Int> {
            return listOf(-10, 10)
        }
    }

    companion object {
        /**
         * Creates new Builder instance used to be initialized from configuration.
         *
         * @return new Builder instance
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}