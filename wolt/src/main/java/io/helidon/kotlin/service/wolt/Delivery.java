package io.helidon.kotlin.service.wolt;

public class Delivery {
    private String id;
    private String food;
    private String address;
    private DeliveryStatus deliveryStatus;

    public Delivery(String id, String food, String address, DeliveryStatus deliveryStatus) {
        this.id = id;
        this.food = food;
        this.address = address;
        this.deliveryStatus = deliveryStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFood() {
        return food;
    }

    public void setFood(String food) {
        this.food = food;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    @Override
    public String toString() {
        return "Delivery{" +
                "id='" + id + '\'' +
                ", food='" + food + '\'' +
                ", address='" + address + '\'' +
                ", deliveryStatus=" + deliveryStatus +
                '}';
    }
}
