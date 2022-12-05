/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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
import io.helidon.config.FileSystemWatcher
import io.helidon.config.PollingStrategies
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

/**
 * Example shows how to use Config accessor methods that return [Supplier].
 * [Supplier] returns always the last loaded config value.
 *
 *
 * The feature is based on using [io.helidon.config.spi.PollingStrategy] with
 * selected config source(s) to check for changes.
 */
class AsSupplierExample {
    private val lastPrinted = AtomicReference<String>()
    private val executor = initExecutor()

    /**
     * Executes the example.
     */
    fun run() {
        val config = Config
                .create(ConfigSources.file("conf/dev.yaml")
                        .optional() // change watcher is a standalone component that watches for
                        // changes and notifies the config system when a change occurs
                        .changeWatcher(FileSystemWatcher.create()),
                        ConfigSources.file("conf/config.yaml")
                                .optional() // polling strategy triggers regular checks on the source to check
                                // for changes, utilizing a concept of "stamp" of the data that is provided
                                // and validated by the source
                                .pollingStrategy(PollingStrategies.regular(Duration.ofSeconds(2))),
                        ConfigSources.classpath("default.yaml"))

        // greeting.get() always return up-to-date value
        val greeting = config["app.greeting"].asString().supplier()
        // name.get() always return up-to-date value
        val name = config["app.name"].asString().supplier()

        // first greeting
        printIfChanged(greeting.get().toString() + " " + name.get() + ".")

        // use same Supplier instances to get up-to-date value
        executor.scheduleWithFixedDelay(
                { printIfChanged(greeting.get().toString() + " " + name.get() + ".") },  // check every 1 second for changes
                0, 1, TimeUnit.SECONDS)
    }

    /**
     * Utility to print same message just once.
     */
    private fun printIfChanged(message: String) {
        lastPrinted.accumulateAndGet(message) { origValue: String?, newValue: String ->
            //print MESSAGE only if changed since the last print
            if (origValue != newValue) {
                LOGGER.info("[AsSupplier] $newValue")
            }
            newValue
        }
    }

    /**
     * Shutdowns executor.
     */
    fun shutdown() {
        executor.shutdown()
    }

    companion object {
        private val LOGGER = Logger.getLogger(AsSupplierExample::class.java.name)
        private fun initExecutor(): ScheduledExecutorService {
            val executor = Executors.newSingleThreadScheduledExecutor()
            Runtime.getRuntime().addShutdownHook(Thread { executor.shutdown() })
            return executor
        }
    }
}