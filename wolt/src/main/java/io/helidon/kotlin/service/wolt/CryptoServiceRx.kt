package io.helidon.kotlin.service.wolt

import io.helidon.common.Base64Value
import io.helidon.common.reactive.Single
import io.helidon.integrations.vault.secrets.transit.Decrypt
import io.helidon.integrations.vault.secrets.transit.Encrypt
import io.helidon.integrations.vault.secrets.transit.TransitSecretsRx
import io.helidon.integrations.vault.sys.EnableEngine
import io.helidon.integrations.vault.sys.SysRx
import io.helidon.kotlin.service.wolt.CryptoServiceRx

class CryptoServiceRx internal constructor(private val sys: SysRx, private val secrets: TransitSecretsRx) {
    init {
        sys.enableEngine(TransitSecretsRx.ENGINE)
            .thenAccept { e: EnableEngine.Response? -> println("Transit Secret engine enabled") }
    }

    fun decryptSecret(encrypted: String?): Single<String> {
        return secrets.decrypt(
            Decrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .cipherText(encrypted)
        )
            .map { response: Decrypt.Response -> response.decrypted().toDecodedString().toString() }
    }

    fun encryptSecret(secret: String?): Single<String> {
        return secrets.encrypt(
            Encrypt.Request.builder()
                .encryptionKeyName(ENCRYPTION_KEY)
                .data(Base64Value.create(secret))
        )
            .map { response: Encrypt.Response -> response.encrypted().cipherText() }
    }

    companion object {
        private const val ENCRYPTION_KEY = "encryption-key"
    }
}