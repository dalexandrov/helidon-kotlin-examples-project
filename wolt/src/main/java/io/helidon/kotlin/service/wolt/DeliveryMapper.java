package io.helidon.kotlin.service.wolt;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryMapper implements DbMapper<Delivery> {

    @Override
    public Delivery read(DbRow row) {
        DbColumn id = row.column("id");
        DbColumn food = row.column("food");
        DbColumn address = row.column("address");
        DbColumn status = row.column("status");
        return new Delivery(id.as(String.class), food.as(String.class), address.as(String.class), status.as(DeliveryStatus.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(Delivery value) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("id", value.getId());
        map.put("food", value.getFood());
        map.put("address", value.getAddress());
        map.put("status", value.getDeliveryStatus().toString());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(Delivery value) {
        List<Object> list = new ArrayList<>(4);
        list.add(value.getId());
        list.add(value.getFood());
        list.add(value.getAddress());
        list.add(value.getDeliveryStatus().toString());
        return list;
    }
}