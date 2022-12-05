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

import io.helidon.common.http.Http
import io.helidon.integrations.vault.secrets.kv2.Kv2Secret
import io.helidon.integrations.vault.secrets.kv2.Kv2SecretsRx
import io.helidon.webserver.*
import java.util.*
import java.util.Map

internal class Kv2Service(private val secrets: Kv2SecretsRx) : Service {
    override fun update(rules: Routing.Rules) {
        rules["/create", Handler { _: ServerRequest, res: ServerResponse ->
            createSecrets(
                res
            )
        }]["/secrets/{path:.*}", Handler { req: ServerRequest, res: ServerResponse -> getSecret(req, res) }]
            .delete("/secrets/{path:.*}", Handler { req: ServerRequest, res: ServerResponse -> deleteSecret(req, res) })
    }

    private fun createSecrets(res: ServerResponse) {
        secrets.create("first/secret", Map.of("key", "secretValue"))
            .thenAccept { res.send("Created secret on path /first/secret") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun deleteSecret(req: ServerRequest, res: ServerResponse) {
        val path = req.path().param("path")
        secrets.deleteAll(path)
            .thenAccept { res.send("Deleted secret on path $path") }
    }

    private fun getSecret(req: ServerRequest, res: ServerResponse) {
        val path = req.path().param("path")
        secrets[path]
            .thenAccept { secret: Optional<Kv2Secret?> ->
                if (secret.isPresent) {
                    // using toString so we do not need to depend on JSON-B
                    val kv2Secret = secret.get()
                    res.send("Version " + kv2Secret.metadata().version() + ", secret: " + kv2Secret.values().toString())
                } else {
                    res.status(Http.Status.NOT_FOUND_404)
                    res.send()
                }
            }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }
}