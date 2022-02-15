package io.helidon.kotlin.service.wolt

import io.helidon.dbclient.DbMapper
import io.helidon.dbclient.DbRow
import java.util.ArrayList
import java.util.HashMap

class DeliveryMapper : DbMapper<Delivery> {
    override fun read(row: DbRow): Delivery {
        val id = row.column("id")
        val food = row.column("food")
        val address = row.column("address")
        val status = row.column("status")
        return Delivery(
            id.`as`(String::class.java), food.`as`(String::class.java), address.`as`(
                String::class.java
            ), status.`as`(DeliveryStatus::class.java)
        )
    }

    override fun toNamedParameters(value: Delivery): Map<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap(1)
        map["id"] = value.id
        map["food"] = value.food
        map["address"] = value.address
        map["status"] = value.deliveryStatus.toString()
        return map
    }

    override fun toIndexedParameters(value: Delivery): List<Any?> {
        val list: MutableList<Any?> = ArrayList(4)
        list.add(value.id)
        list.add(value.food)
        list.add(value.address)
        list.add(value.deliveryStatus.toString())
        return list
    }
}