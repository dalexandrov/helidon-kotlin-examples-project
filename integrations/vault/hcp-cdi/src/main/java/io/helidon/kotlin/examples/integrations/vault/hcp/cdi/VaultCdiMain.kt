/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.integrations.vault.hcp.cdi

import io.helidon.config.yaml.mp.YamlMpConfigSource
import io.helidon.microprofile.cdi.Main
import org.eclipse.microprofile.config.spi.ConfigProviderResolver
import org.eclipse.microprofile.config.spi.ConfigSource
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Main class of example.
 */
object VaultCdiMain {
    /**
     * Main method of example.
     *
     * @param args ignored
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val configProvider = ConfigProviderResolver.instance()
        val mpConfig = configProvider.builder
            .addDefaultSources()
            .withSources(*examplesConfig())
            .addDiscoveredSources()
            .addDiscoveredConverters()
            .build()

        // configure
        configProvider.registerConfig(mpConfig, null)

        // start CDI
        Main.main(args)
    }

    private fun examplesConfig(): Array<ConfigSource?> {
        val path = Paths.get(System.getProperty("user.home") + "/helidon/conf/examples.yaml")
        return if (Files.exists(path)) {
            arrayOf(
                YamlMpConfigSource.create(
                    path
                )
            )
        } else arrayOfNulls(0)
    }
}