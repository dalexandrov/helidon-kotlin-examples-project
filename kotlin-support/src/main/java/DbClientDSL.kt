import io.helidon.dbclient.DbClient

fun dbClient(block: DbClient.Builder.() -> Unit): DbClient = DbClient.builder().apply(block).build()