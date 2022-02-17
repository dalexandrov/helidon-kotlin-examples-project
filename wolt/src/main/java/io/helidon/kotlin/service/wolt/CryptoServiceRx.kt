package io.helidon.kotlin.service.wolt

import io.helidon.common.Base64Value
import io.helidon.common.reactive.Single
import io.helidon.integrations.vault.secrets.transit.CreateKey
import io.helidon.integrations.vault.secrets.transit.Decrypt
import io.helidon.integrations.vault.secrets.transit.Encrypt
import io.helidon.integrations.vault.secrets.transit.TransitSecretsRx
import io.helidon.integrations.vault.sys.EnableEngine
import io.helidon.integrations.vault.sys.SysRx

class CryptoServiceRx internal constructor(private val sys: SysRx, private val secrets: TransitSecretsRx) {

    init {
        val request = CreateKey.Request.builder()
        .name(ENCRYPTION_KEY)

        sys.enableEngine(TransitSecretsRx.ENGINE)
            .thenCompose {
                secrets.createKey(request)
                    .flatMapSingle { ignored: CreateKey.Response? ->
                        secrets.createKey(
                            CreateKey.Request.builder()
                                .name(SIGNATURE_KEY)
                                .type("rsa-2048")
                        )
                    }
            }
            .thenAccept { println("Transit Secret engine enabled") }
            .exceptionallyAccept{ e-> println(e.toString()) }

    }

    fun decryptSecret(encrypted: String): Single<String> {
        return secrets.decrypt(
            Decrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .cipherText(encrypted)
        )
            .map { response: Decrypt.Response -> response.decrypted().toDecodedString().toString() }
    }

    fun encryptSecret(secret: String): Single<String> {
        return secrets.encrypt(
            Encrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .data(Base64Value.create(secret))
        )
            .map { response: Encrypt.Response -> response.encrypted().cipherText() }
    }

    companion object {
        private const val ENCRYPTION_KEY = "encryption-key"
        private const val SIGNATURE_KEY = "signature-key"
    }
}