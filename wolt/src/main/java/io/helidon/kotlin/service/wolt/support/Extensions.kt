package support;

import io.helidon.common.mapper.MapperException
import io.helidon.common.reactive.Single
import io.helidon.config.Config
import io.helidon.config.ConfigValue
import io.helidon.dbclient.DbColumn
import io.helidon.dbclient.DbRow
import io.helidon.media.common.MessageBodyReadableContent
import java.util.function.Function

/**
 * Extension function to hide keyword `as`.
 */
inline fun <reified T> MessageBodyReadableContent.single(): Single<T> {
    return this.`as`(T::class.java)
}

inline fun <reified T> DbRow.to(): T {
    return this.`as`(T::class.java)
}

inline fun <reified T> Config.to(): ConfigValue<T> {
    return this.`as`(T::class.java);
}

@Throws(MapperException::class)
inline fun <reified T> DbColumn.to(): T {
    return this.`as`(T::class.java)
}



fun <T> Config.toType(type: Function<Config, T>): ConfigValue<T> {
    return this.`as`(type);
}