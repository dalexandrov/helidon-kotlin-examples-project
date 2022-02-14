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

import io.helidon.common.reactive.Single
import io.helidon.config.Config
import io.helidon.integrations.common.rest.ApiResponse
import io.helidon.integrations.vault.Vault
import io.helidon.integrations.vault.auths.approle.AppRoleAuthRx
import io.helidon.integrations.vault.auths.approle.AppRoleVaultAuth
import io.helidon.integrations.vault.auths.approle.CreateAppRole
import io.helidon.integrations.vault.auths.approle.GenerateSecretId
import io.helidon.integrations.vault.secrets.kv2.Kv2Secret
import io.helidon.integrations.vault.secrets.kv2.Kv2SecretsRx
import io.helidon.integrations.vault.sys.EnableAuth
import io.helidon.integrations.vault.sys.SysRx
import java.time.Duration
import java.util.*
import java.util.Map
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

internal class AppRoleExample(private val tokenVault: Vault, private val config: Config) {
    private val sys: SysRx = tokenVault.sys(SysRx.API)
    private var appRoleVault: Vault? = null

    fun run(): Single<String> {
        /*
         The following tasks must be run before we authenticate
         */
        return enableAppRoleAuth() // Now we can login using AppRole
            .flatMapSingle { workWithSecrets() } // Now back to token based Vault, as we will clean up
            .flatMapSingle { disableAppRoleAuth() }
            .map { "AppRole example finished successfully." }
    }

    private fun workWithSecrets(): Single<ApiResponse?> {
        val secrets = appRoleVault!!.secrets(Kv2SecretsRx.ENGINE)
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
                    println("appRole first secret: " + kv2Secret.value("secret-key"))
                    println("appRole second secret: " + kv2Secret.value("secret-user"))
                } else {
                    println("appRole secret not found")
                }
            }.flatMapSingle { secrets.deleteAll(SECRET_PATH) }
    }

    private fun disableAppRoleAuth(): Single<ApiResponse?> {
        return sys.deletePolicy(POLICY_NAME)
            .flatMapSingle { sys.disableAuth(CUSTOM_APP_ROLE_PATH) }
    }

    private fun enableAppRoleAuth(): Single<String> {
        val roleId = AtomicReference<String>()
        val secretId = AtomicReference<String>()

        // enable the method
        return sys.enableAuth(
            EnableAuth.Request.builder()
                .auth(AppRoleAuthRx.AUTH_METHOD) // must be aligned with path configured in application.yaml
                .path(CUSTOM_APP_ROLE_PATH)
        ) // add policy
            .flatMapSingle { sys.createPolicy(POLICY_NAME, VaultPolicy.POLICY) }
            .flatMapSingle {
                tokenVault.auth(AppRoleAuthRx.AUTH_METHOD, CUSTOM_APP_ROLE_PATH)
                    .createAppRole(
                        CreateAppRole.Request.builder()
                            .roleName(ROLE_NAME)
                            .addTokenPolicy(POLICY_NAME)
                            .tokenExplicitMaxTtl(Duration.ofMinutes(1))
                    )
            }
            .flatMapSingle {
                tokenVault.auth(AppRoleAuthRx.AUTH_METHOD, CUSTOM_APP_ROLE_PATH)
                    .readRoleId(ROLE_NAME)
            }
            .peek { it.ifPresent { newValue: String -> roleId.set(newValue) } }
            .flatMapSingle {
                tokenVault.auth(AppRoleAuthRx.AUTH_METHOD, CUSTOM_APP_ROLE_PATH)
                    .generateSecretId(
                        GenerateSecretId.Request.builder()
                            .roleName(ROLE_NAME)
                            .addMetadata("name", "helidon")
                    )
            }
            .map(Function { response: GenerateSecretId.Response -> response.secretId() })
            .peek { newValue: String -> secretId.set(newValue) }
            .peek {
                println("roleId: " + roleId.get())
                println("secretId: " + secretId.get())
                appRoleVault = Vault.builder()
                    .config(config)
                    .addVaultAuth(
                        AppRoleVaultAuth.builder()
                            .path(CUSTOM_APP_ROLE_PATH)
                            .appRoleId(roleId.get())
                            .secretId(secretId.get())
                            .build()
                    )
                    .build()
            }
    }

    companion object {
        private const val SECRET_PATH = "approle/example/secret"
        private const val ROLE_NAME = "approle_role"
        private const val POLICY_NAME = "approle_policy"
        private const val CUSTOM_APP_ROLE_PATH = "customapprole"
    }
}