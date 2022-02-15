package io.helidon.kotlin.service.wolt;

import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.spi.DbMapperProvider;

import javax.annotation.Priority;
import java.util.Optional;

@Priority(1000)
public class DeliveryMapperProvider implements DbMapperProvider {
    private static final DeliveryMapper MAPPER = new DeliveryMapper();

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(Delivery.class)) {
            return Optional.of((DbMapper<T>) MAPPER);
        }
        return Optional.empty();
    }
}
