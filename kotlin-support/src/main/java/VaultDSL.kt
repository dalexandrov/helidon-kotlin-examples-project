import io.helidon.integrations.vault.Vault
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webclient.WebClient


/**
 * DSL for the builder for HashiCorp Vault.
 */
fun vault(block: Vault.Builder.() -> Unit = {}): Vault = Vault.builder().apply(block).build()
