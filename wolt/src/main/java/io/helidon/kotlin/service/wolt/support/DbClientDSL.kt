package support;

import io.helidon.dbclient.DbClient

/**
 * DSL for the builder for DBClient.
 */
fun dbClient(block: DbClient.Builder.() -> Unit = {}): DbClient = DbClient.builder().apply(block).build()