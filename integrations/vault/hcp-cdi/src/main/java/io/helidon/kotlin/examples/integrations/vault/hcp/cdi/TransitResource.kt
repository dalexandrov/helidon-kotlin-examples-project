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
package io.helidon.kotlin.examples.integrations.vault.hcp.cdi

import io.helidon.common.Base64Value
import io.helidon.integrations.vault.secrets.transit.*
import io.helidon.integrations.vault.sys.Sys
import javax.inject.Inject
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response

/**
 * JAX-RS resource for Transit secrets engine operations.
 */
@Path("/transit")
open class TransitResource @Inject internal constructor(private val sys: Sys, private val secrets: TransitSecrets) {
    /**
     * Enable the secrets engine on the default path.
     *
     * @return response
     */
    @Path("/engine")
    @GET
    open fun enableEngine(): Response {
        val response = sys.enableEngine(TransitSecretsRx.ENGINE)
        return Response.ok()
            .entity("Transit secret engine is now enabled. Original status: " + response.status().code())
            .build()
    }

    /**
     * Disable the secrets engine on the default path.
     * @return response
     */
    @Path("/engine")
    @DELETE
    open fun disableEngine(): Response {
        val response = sys.disableEngine(TransitSecretsRx.ENGINE)
        return Response.ok()
            .entity("Transit secret engine is now disabled. Original status: " + response.status())
            .build()
    }

    /**
     * Create the encrypting and signature keys.
     *
     * @return response
     */
    @Path("/keys")
    @GET
    open fun createKeys(): Response {
        secrets.createKey(
            CreateKey.Request.builder()
                .name(ENCRYPTION_KEY)
        )
        secrets.createKey(
            CreateKey.Request.builder()
                .name(SIGNATURE_KEY)
                .type("rsa-2048")
        )
        return Response.ok()
            .entity("Created encryption (and HMAC), and signature keys")
            .build()
    }

    /**
     * Delete the encryption and signature keys.
     *
     * @return response
     */
    @Path("/keys")
    @DELETE
    open fun deleteKeys(): Response {
        // we must first enable deletion of the key (by default it cannot be deleted)
        secrets.updateKeyConfig(
            UpdateKeyConfig.Request.builder()
                .name(ENCRYPTION_KEY)
                .allowDeletion(true)
        )
        secrets.updateKeyConfig(
            UpdateKeyConfig.Request.builder()
                .name(SIGNATURE_KEY)
                .allowDeletion(true)
        )
        secrets.deleteKey(DeleteKey.Request.create(ENCRYPTION_KEY))
        secrets.deleteKey(DeleteKey.Request.create(SIGNATURE_KEY))
        return Response.ok()
            .entity("Deleted encryption (and HMAC), and signature keys")
            .build()
    }

    /**
     * Encrypt a secret.
     *
     * @param secret provided as part of the path
     * @return cipher text
     */
    @Path("/encrypt/{secret: .*}")
    @GET
    open fun encryptSecret(@PathParam("secret") secret: String?): String {
        return secrets.encrypt(
            Encrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .data(Base64Value.create(secret))
        )
            .encrypted()
            .cipherText()
    }

    /**
     * Decrypt a secret.
     *
     * @param cipherText provided as part of the path
     * @return decrypted secret text
     */
    @Path("/decrypt/{cipherText: .*}")
    @GET
    open fun decryptSecret(@PathParam("cipherText") cipherText: String?): String {
        return secrets.decrypt(
            Decrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .cipherText(cipherText)
        )
            .decrypted()
            .toDecodedString()
    }

    /**
     * Create an HMAC for text.
     *
     * @param text text to do HMAC for
     * @return hmac string that can be used to [.verifyHmac]
     */
    @Path("/hmac/{text}")
    @GET
    open fun hmac(@PathParam("text") text: String?): String {
        return secrets.hmac(
            Hmac.Request.builder()
                .hmacKeyName(ENCRYPTION_KEY)
                .data(Base64Value.create(text))
        )
            .hmac()
    }

    /**
     * Create a signature for text.
     *
     * @param text text to sign
     * @return signature string that can be used to [.verifySignature]
     */
    @Path("/sign/{text}")
    @GET
    open fun sign(@PathParam("text") text: String?): String {
        return secrets.sign(
            Sign.Request.builder()
                .signatureKeyName(SIGNATURE_KEY)
                .data(Base64Value.create(text))
        )
            .signature()
    }

    /**
     * Verify HMAC.
     *
     * @param secret secret that was used to [.hmac]
     * @param hmac HMAC text
     * @return `HMAC Valid` or `HMAC Invalid`
     */
    @Path("/verify/hmac/{secret}/{hmac: .*}")
    @GET
    open fun verifyHmac(@PathParam("secret") secret: String?, @PathParam("hmac") hmac: String?): String {
        val isValid = secrets.verify(
            Verify.Request.builder()
                .digestKeyName(ENCRYPTION_KEY)
                .data(Base64Value.create(secret))
                .hmac(hmac)
        )
            .isValid
        return if (isValid) "HMAC Valid" else "HMAC Invalid"
    }

    /**
     * Verify signature.
     *
     * @param secret secret that was used to [.sign]
     * @param signature signature
     * @return `Signature Valid` or `Signature Invalid`
     */
    @Path("/verify/sign/{secret}/{signature: .*}")
    @GET
    open fun verifySignature(@PathParam("secret") secret: String?, @PathParam("signature") signature: String?): String {
        val isValid = secrets.verify(
            Verify.Request.builder()
                .digestKeyName(SIGNATURE_KEY)
                .data(Base64Value.create(secret))
                .signature(signature)
        )
            .isValid
        return if (isValid) "Signature Valid" else "Signature Invalid"
    }

    companion object {
        private const val ENCRYPTION_KEY = "encryption-key"
        private const val SIGNATURE_KEY = "signature-key"
    }
}