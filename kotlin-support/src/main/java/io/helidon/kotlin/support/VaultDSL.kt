import io.helidon.integrations.vault.Vault


/**
 * DSL for the builder for HashiCorp Vault.
 */
fun vault(block: Vault.Builder.() -> Unit = {}): Vault = Vault.builder().apply(block).build()
