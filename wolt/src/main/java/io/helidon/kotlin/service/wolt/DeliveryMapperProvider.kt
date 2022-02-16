package io.helidon.kotlin.service.wolt

import io.helidon.dbclient.DbMapper
import io.helidon.dbclient.spi.DbMapperProvider
import java.util.*
import javax.annotation.Priority

@Priority(1000)
class DeliveryMapperProvider : DbMapperProvider {
    override fun <T> mapper(type: Class<T>): Optional<DbMapper<T>> {
        return if (type == Delivery::class.java) {
            Optional.of(MAPPER as DbMapper<T>)
        } else Optional.empty()
    }

    companion object {
        private val MAPPER = DeliveryMapper()
    }
}