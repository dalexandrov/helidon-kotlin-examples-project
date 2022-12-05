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

import io.helidon.common.Base64Value
import io.helidon.integrations.vault.secrets.transit.*
import io.helidon.integrations.vault.secrets.transit.Encrypt.Encrypted
import io.helidon.integrations.vault.sys.SysRx
import io.helidon.webserver.*

internal class TransitService(private val sys: SysRx, private val secrets: TransitSecretsRx) : Service {
    override fun update(rules: Routing.Rules) {
        rules["/enable", Handler { _: ServerRequest, res: ServerResponse ->
            enableEngine(
                res
            )
        }]["/keys", Handler { _: ServerRequest, res: ServerResponse -> createKeys(res) }]
            .delete(
                "/keys",
                Handler { _: ServerRequest, res: ServerResponse ->
                    deleteKeys(
                        res
                    )
                })["/batch", Handler { _: ServerRequest, res: ServerResponse ->
            batch(
                res
            )
        }]["/encrypt/{text:.*}", Handler { req: ServerRequest, res: ServerResponse ->
            encryptSecret(
                req,
                res
            )
        }]["/decrypt/{text:.*}", Handler { req: ServerRequest, res: ServerResponse ->
            decryptSecret(
                req,
                res
            )
        }]["/sign", Handler { _: ServerRequest, res: ServerResponse ->
            sign(
                res
            )
        }]["/hmac", Handler { _: ServerRequest, res: ServerResponse ->
            hmac(
                res
            )
        }]["/verify/sign/{text:.*}", Handler { req: ServerRequest, res: ServerResponse ->
            verify(
                req,
                res
            )
        }]["/verify/hmac/{text:.*}", Handler { req: ServerRequest, res: ServerResponse ->
            verifyHmac(
                req,
                res
            )
        }]["/disable", Handler { _: ServerRequest, res: ServerResponse -> disableEngine(res) }]
    }

    private fun enableEngine(res: ServerResponse) {
        sys.enableEngine(TransitSecretsRx.ENGINE)
            .thenAccept { res.send("Transit Secret engine enabled") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun disableEngine(res: ServerResponse) {
        sys.disableEngine(TransitSecretsRx.ENGINE)
            .thenAccept { res.send("Transit Secret engine disabled") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun createKeys(res: ServerResponse) {
        val request = CreateKey.Request.builder()
            .name(ENCRYPTION_KEY)
        secrets.createKey(request)
            .flatMapSingle {
                secrets.createKey(
                    CreateKey.Request.builder()
                        .name(SIGNATURE_KEY)
                        .type("rsa-2048")
                )
            }
            .forSingle { res.send("Created keys") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun deleteKeys(res: ServerResponse) {
        secrets.updateKeyConfig(
            UpdateKeyConfig.Request.builder()
                .name(ENCRYPTION_KEY)
                .allowDeletion(true)
        )
            .peek { println("Updated key config") }
            .flatMapSingle {
                secrets.deleteKey(
                    DeleteKey.Request.create(
                        ENCRYPTION_KEY
                    )
                )
            }
            .forSingle { res.send("Deleted key.") }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun decryptSecret(req: ServerRequest, res: ServerResponse) {
        val encrypted = req.path().param("text")
        secrets.decrypt(
            Decrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .cipherText(encrypted)
        )
            .forSingle { response: Decrypt.Response -> res.send(response.decrypted().toDecodedString().toString()) }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun encryptSecret(req: ServerRequest, res: ServerResponse) {
        val secret = req.path().param("text")
        secrets.encrypt(
            Encrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .data(Base64Value.create(secret))
        )
            .forSingle { response: Encrypt.Response -> res.send(response.encrypted().cipherText()) }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun hmac(res: ServerResponse) {
        secrets.hmac(
            Hmac.Request.builder()
                .hmacKeyName(ENCRYPTION_KEY)
                .data(SECRET_STRING)
        )
            .forSingle { response: Hmac.Response -> res.send(response.hmac()) }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun sign(res: ServerResponse) {
        secrets.sign(
            Sign.Request.builder()
                .signatureKeyName(SIGNATURE_KEY)
                .data(SECRET_STRING)
        )
            .forSingle { response: Sign.Response -> res.send(response.signature()) }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun verifyHmac(req: ServerRequest, res: ServerResponse) {
        val hmac = req.path().param("text")
        secrets.verify(
            Verify.Request.builder()
                .digestKeyName(ENCRYPTION_KEY)
                .data(SECRET_STRING)
                .hmac(hmac)
        )
            .forSingle { response: Verify.Response -> res.send("Valid: " + response.isValid) }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun verify(req: ServerRequest, res: ServerResponse) {
        val signature = req.path().param("text")
        secrets.verify(
            Verify.Request.builder()
                .digestKeyName(SIGNATURE_KEY)
                .data(SECRET_STRING)
                .signature(signature)
        )
            .forSingle { response: Verify.Response -> res.send("Valid: " + response.isValid) }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    private fun batch(res: ServerResponse) {
        val data = arrayOf("one", "two", "three", "four")
        val request = EncryptBatch.Request.builder()
            .encryptionKeyName(ENCRYPTION_KEY)
        val decryptRequest = DecryptBatch.Request.builder()
            .encryptionKeyName(ENCRYPTION_KEY)
        for (datum in data) {
            request.addEntry(EncryptBatch.BatchEntry.create(Base64Value.create(datum)))
        }
        secrets.encrypt(request)
            .map { response: EncryptBatch.Response -> response.batchResult() }
            .flatMapSingle { batchResult: List<Encrypted> ->
                for (encrypted in batchResult) {
                    println("Encrypted: " + encrypted.cipherText())
                    decryptRequest.addEntry(DecryptBatch.BatchEntry.create(encrypted.cipherText()))
                }
                secrets.decrypt(decryptRequest)
            }
            .forSingle { response: DecryptBatch.Response ->
                val base64Values = response.batchResult()
                for (i in data.indices) {
                    val decryptedValue = base64Values[i].toDecodedString()
                    if (data[i] != decryptedValue) {
                        res.send(
                            "Data at index " + i + " is invalid. Decrypted " + decryptedValue
                                    + ", expected: " + data[i]
                        )
                        return@forSingle
                    }
                }
                res.send("Batch encryption/decryption completed")
            }
            .exceptionally { throwable: Throwable? -> res.send(throwable) }
    }

    companion object {
        private const val ENCRYPTION_KEY = "encryption-key"
        private const val SIGNATURE_KEY = "signature-key"
        private val SECRET_STRING = Base64Value.create("Hello World")
    }
}