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

import io.helidon.common.http.Http
import io.helidon.integrations.vault.Secret
import io.helidon.integrations.vault.secrets.kv1.Kv1SecretsRx
import io.helidon.integrations.vault.sys.SysRx
import io.helidon.webserver.*
import java.util.*
import java.util.Map

internal class Kv1Service(private val sys: SysRx, private val secrets: Kv1SecretsRx) : Service {
    override fun update(rules: Routing.Rules) {
        rules["/enable", Handler { _: ServerRequest, res: ServerResponse ->
            enableEngine(
                res
            )
        }]["/create", Handler { _: ServerRequest, res: ServerResponse ->
            createSecrets(
                res
            )
        }]["/secrets/{path:.*}", Handler { req: ServerRequest, res: ServerResponse -> getSecret(req, res) }]
            .delete(
                "/secrets/{path:.*}",
                Handler { req: ServerRequest, res: ServerResponse ->
                    deleteSecret(
                        req,
                        res
                    )
                })["/disable", Handler { _: ServerRequest, res: ServerResponse -> disableEngine(res) }]
    }

    private fun disableEngine(res: ServerResponse) {
        sys.disableEngine(Kv1SecretsRx.ENGINE)
            .thenAccept { res.send("KV1 Secret engine disabled") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun enableEngine(res: ServerResponse) {
        sys.enableEngine(Kv1SecretsRx.ENGINE)
            .thenAccept { res.send("KV1 Secret engine enabled") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun createSecrets(res: ServerResponse) {
        secrets.create("first/secret", Map.of("key", "secretValue"))
            .thenAccept { res.send("Created secret on path /first/secret") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun deleteSecret(req: ServerRequest, res: ServerResponse) {
        val path = req.path().param("path")
        secrets.delete(path)
            .thenAccept { res.send("Deleted secret on path $path") }
    }

    private fun getSecret(req: ServerRequest, res: ServerResponse) {
        val path = req.path().param("path")
        secrets[path]
            .thenAccept { secret: Optional<Secret> ->
                if (secret.isPresent) {
                    // using toString so we do not need to depend on JSON-B
                    res.send(secret.get().values().toString())
                } else {
                    res.status(Http.Status.NOT_FOUND_404)
                    res.send()
                }
            }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }
}