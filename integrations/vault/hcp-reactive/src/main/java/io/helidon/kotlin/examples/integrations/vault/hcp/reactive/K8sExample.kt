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
package io.helidon.kotlin.examples.integrations.vault.hcp.reactive

import io.helidon.common.reactive.Single
import io.helidon.config.Config
import io.helidon.integrations.common.rest.ApiResponse
import io.helidon.integrations.vault.Vault
import io.helidon.integrations.vault.auths.k8s.ConfigureK8s
import io.helidon.integrations.vault.auths.k8s.CreateRole
import io.helidon.integrations.vault.auths.k8s.K8sAuthRx
import io.helidon.integrations.vault.secrets.kv2.Kv2Secret
import io.helidon.integrations.vault.secrets.kv2.Kv2SecretsRx
import io.helidon.integrations.vault.sys.SysRx
import java.util.*
import java.util.Map
import java.util.function.Function

internal class K8sExample(private val tokenVault: Vault, config: Config) {
    private val k8sAddress: String
    private val config: Config
    private val sys: SysRx
    private var k8sVault: Vault? = null

    init {
        sys = tokenVault.sys(SysRx.API)
        k8sAddress = config["cluster-address"].asString().get()
        this.config = config
    }

    fun run(): Single<String> {
        /*
         The following tasks must be run before we authenticate
         */
        return enableK8sAuth() // Now we can login using k8s - must run within a k8s cluster (or you need the k8s configuration files locally)
            .flatMapSingle { workWithSecrets() } // Now back to token based Vault, as we will clean up
            .flatMapSingle { disableK8sAuth() }
            .map { "k8s example finished successfully." }
    }

    private fun workWithSecrets(): Single<ApiResponse?> {
        val secrets = k8sVault!!.secrets(Kv2SecretsRx.ENGINE)
        return secrets.create(
            SECRET_PATH, Map.of(
                "secret-key", "secretValue",
                "secret-user", "username"
            )
        )
            .flatMapSingle { secrets[SECRET_PATH] }
            .peek { secret: Optional<Kv2Secret> ->
                if (secret.isPresent) {
                    val kv2Secret = secret.get()
                    println("k8s first secret: " + kv2Secret.value("secret-key"))
                    println("k8s second secret: " + kv2Secret.value("secret-user"))
                } else {
                    println("k8s secret not found")
                }
            }.flatMapSingle { secrets.deleteAll(SECRET_PATH) }
    }

    private fun disableK8sAuth(): Single<ApiResponse?> {
        return sys.deletePolicy(POLICY_NAME)
            .flatMapSingle { sys.disableAuth(K8sAuthRx.AUTH_METHOD.defaultPath()) }
    }

    private fun enableK8sAuth(): Single<ApiResponse> {
        // enable the method
        return sys.enableAuth(K8sAuthRx.AUTH_METHOD) // add policy
            .flatMapSingle { sys.createPolicy(POLICY_NAME, VaultPolicy.POLICY) }
            .flatMapSingle {
                tokenVault.auth(K8sAuthRx.AUTH_METHOD)
                    .configure(
                        ConfigureK8s.Request.builder()
                            .address(k8sAddress)
                    )
            }
            .flatMapSingle {
                tokenVault.auth(K8sAuthRx.AUTH_METHOD) // this must be the same role name as is defined in application.yaml
                    .createRole(
                        CreateRole.Request.builder()
                            .roleName("my-role")
                            .addBoundServiceAccountName("*")
                            .addBoundServiceAccountNamespace("default")
                            .addTokenPolicy(POLICY_NAME)
                    )
            }
            .peek { k8sVault = Vault.create(config) }
            .map(Function.identity())
    }

    companion object {
        private const val SECRET_PATH = "k8s/example/secret"
        private const val POLICY_NAME = "k8s_policy"
    }
}