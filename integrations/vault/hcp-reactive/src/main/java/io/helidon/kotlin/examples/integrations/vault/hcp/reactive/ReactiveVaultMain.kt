/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.integrations.vault.hcp.reactive

import io.helidon.common.LogConfig
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.integrations.vault.Vault
import io.helidon.integrations.vault.secrets.cubbyhole.CubbyholeSecretsRx
import io.helidon.integrations.vault.secrets.kv1.Kv1SecretsRx
import io.helidon.integrations.vault.secrets.kv2.Kv2SecretsRx
import io.helidon.integrations.vault.secrets.transit.TransitSecretsRx
import io.helidon.integrations.vault.sys.SysRx
import io.helidon.webclient.WebClient
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.util.concurrent.TimeUnit

/**
 * Main class of example.
 */
object ReactiveVaultMain {
    /**
     * Main method of example.
     *
     * @param args ignored
     */
    @JvmStatic
    fun main(args: Array<String>) {
        LogConfig.configureRuntime()

        // as I cannot share my secret configuration, let's combine the configuration
        // from my home directory with the one compiled into the jar
        // when running this example, you can either update the application.yaml in resources directory
        // or use the same approach
        val config = buildConfig()

        // we have three configurations available
        // 1. Token based authentication
        val tokenVault = Vault.builder()
            .config(config["vault.token"])
            .updateWebClient { it: WebClient.Builder ->
                it.connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
            }
            .build()

        // 2. App role based authentication - must be created after we obtain the role id an token
        // 3. Kubernetes (k8s) based authentication (requires to run on k8s) - must be created after we create
        //      the authentication method

        // the tokenVault is using the root token and can be used to enable engines and
        // other authentication mechanisms
        val k8sFuture = K8sExample(tokenVault, config["vault.k8s"])
            .run()
            .forSingle { x: String? -> println(x) }
        val appRoleFuture = AppRoleExample(tokenVault, config["vault.approle"])
            .run()
            .forSingle { x: String? -> println(x) }

        /*
        We do not need to block here for our examples, as the server started below will keep the process running
         */
        val sys = tokenVault.sys(SysRx.API)
        // we use await for webserver, as we do not care if we block the main thread - it is not used
        // for anything
        val webServer = WebServer.builder()
            .config(config["server"])
            .routing(
                Routing.builder()
                    .register("/cubbyhole", CubbyholeService(sys, tokenVault.secrets(CubbyholeSecretsRx.ENGINE)))
                    .register("/kv1", Kv1Service(sys, tokenVault.secrets(Kv1SecretsRx.ENGINE)))
                    .register("/kv2", Kv2Service(tokenVault.secrets(Kv2SecretsRx.ENGINE)))
                    .register("/transit", TransitService(sys, tokenVault.secrets(TransitSecretsRx.ENGINE)))
            )
            .build()
            .start()
            .await()
        try {
            appRoleFuture.await()
        } catch (e: Exception) {
            System.err.println("AppRole example failed")
            e.printStackTrace()
        }
        try {
            k8sFuture.await()
        } catch (e: Exception) {
            System.err.println("Kubernetes example failed")
            e.printStackTrace()
        }
        val baseAddress = "http://localhost:" + webServer.port() + "/"
        println("Server started on $baseAddress")
        println()
        println("Key/Value Version 1 Secrets Engine")
        println("\t" + baseAddress + "kv1/enable")
        println("\t" + baseAddress + "kv1/create")
        println("\t" + baseAddress + "kv1/secrets/first/secret")
        println("\tcurl -i -X DELETE " + baseAddress + "kv1/secrets/first/secret")
        println("\t" + baseAddress + "kv1/disable")
        println()
        println("Key/Value Version 2 Secrets Engine")
        println("\t" + baseAddress + "kv2/create")
        println("\t" + baseAddress + "kv2/secrets/first/secret")
        println("\tcurl -i -X DELETE " + baseAddress + "kv2/secrets/first/secret")
        println()
        println("Transit Secrets Engine")
        println("\t" + baseAddress + "transit/enable")
        println("\t" + baseAddress + "transit/keys")
        println("\t" + baseAddress + "transit/encrypt/secret_text")
        println("\t" + baseAddress + "transit/decrypt/cipher_text")
        println("\t" + baseAddress + "transit/sign")
        println("\t" + baseAddress + "transit/verify/sign/signature_text")
        println("\t" + baseAddress + "transit/hmac")
        println("\t" + baseAddress + "transit/verify/hmac/hmac_text")
        println("\tcurl -i -X DELETE " + baseAddress + "transit/keys")
        println("\t" + baseAddress + "transit/disable")
    }

    private fun buildConfig(): Config {
        return Config.builder()
            .sources( // you can use this file to override the defaults that are built-in
                ConfigSources.file(System.getProperty("user.home") + "/helidon/conf/examples.yaml")
                    .optional(),  // in jar file (see src/main/resources/application.yaml)
                ConfigSources.classpath("application.yaml")
            )
            .build()
    }
}