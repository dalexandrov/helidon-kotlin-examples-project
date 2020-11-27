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

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Config changes examples.
 */
object Main {
    private val LOGGER = Logger.getLogger(Main::class.java.name)

    /**
     * Executes the example.
     *
     * @param args arguments
     * @throws InterruptedException in case you cannot sleep
     * @throws IOException          in case of IO error
     */
    @Throws(IOException::class, InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // subscribe using simple onChange function
        OnChangeExample().run()
        // use same Supplier instances to get up-to-date value
        val asSupplier = AsSupplierExample()
        asSupplier.run()

        // waiting for user made changes in config files
        val sleep: Long = 60
        LOGGER.info("Application is waiting $sleep seconds for change...")
        TimeUnit.SECONDS.sleep(sleep)
        asSupplier.shutdown()
        LOGGER.info("Goodbye.")
    }
}