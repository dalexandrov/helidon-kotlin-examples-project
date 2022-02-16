package io.helidon.kotlin.service.wolt

class Delivery(var id: String, var food: String, var address: String, var deliveryStatus: DeliveryStatus) {

    override fun toString(): String {
        return "Delivery{" +
                "id='" + id + '\'' +
                ", food='" + food + '\'' +
                ", address='" + address + '\'' +
                ", deliveryStatus=" + deliveryStatus +
                '}'
    }
}