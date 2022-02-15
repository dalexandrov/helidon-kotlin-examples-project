package io.helidon.kotlin.service.wolt;

import io.helidon.common.Base64Value;
import io.helidon.common.reactive.Single;
import io.helidon.integrations.vault.secrets.transit.Decrypt;
import io.helidon.integrations.vault.secrets.transit.Encrypt;
import io.helidon.integrations.vault.secrets.transit.TransitSecretsRx;
import io.helidon.integrations.vault.sys.SysRx;

public class CryptoServiceRx {

    private static final String ENCRYPTION_KEY = "encryption-key";
    private final SysRx sys;
    private final TransitSecretsRx secrets;

    CryptoServiceRx(SysRx sys, TransitSecretsRx secrets) {
        this.sys = sys;
        this.secrets = secrets;

        sys.enableEngine(TransitSecretsRx.ENGINE)
                .thenAccept(e-> System.out.println("Transit Secret engine enabled"));
    }

    public Single<String> decryptSecret(String encrypted) {

        return secrets.decrypt(Decrypt.Request.builder()
                        .encryptionKeyName(ENCRYPTION_KEY)
                        .cipherText(encrypted))
                        .map(response -> String.valueOf(response.decrypted().toDecodedString()));

    }

    public Single<String> encryptSecret(String secret) {
        return secrets.encrypt(Encrypt.Request.builder()
                        .encryptionKeyName(ENCRYPTION_KEY)
                        .data(Base64Value.create(secret)))
                .map(response -> response.encrypted().cipherText());
    }
}
