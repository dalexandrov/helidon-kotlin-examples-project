package io.helidon.kotlin.service.wolt

import io.helidon.dbclient.DbMapper
import io.helidon.dbclient.DbRow
import support.to
import java.util.ArrayList
import java.util.HashMap


class DeliveryMapper : DbMapper<Delivery> {

    override fun read(row: DbRow): Delivery {
        val id = row.column("id").to<String>()
        val food = row.column("food").to<String>()
        val address = row.column("address").to<String>()
        val status = row.column("status").to<DeliveryStatus>()
        return Delivery(
            id, food, address, status
        )
    }

    override fun toNamedParameters(value: Delivery): Map<String, Any> {
        val map: MutableMap<String, Any> = HashMap(1)
        map["id"] = value.id
        map["food"] = value.food
        map["address"] = value.address
        map["status"] = value.deliveryStatus.toString()
        return map
    }

    override fun toIndexedParameters(value: Delivery): List<Any> {
        val list: MutableList<Any> = ArrayList(4)
        list.add(value.id)
        list.add(value.food)
        list.add(value.address)
        list.add(value.deliveryStatus.toString())
        return list
    }
}