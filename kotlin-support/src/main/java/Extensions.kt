import io.helidon.common.GenericType
import io.helidon.common.mapper.MapperException
import io.helidon.common.reactive.Single
import io.helidon.config.Config
import io.helidon.config.ConfigValue
import io.helidon.dbclient.DbColumn
import io.helidon.dbclient.DbRow
import io.helidon.media.common.MessageBodyReadableContent
import io.helidon.media.multipart.ReadableBodyPart
import java.util.function.Function

/**
 * Extension function to hide keyword `as`.
 */
inline fun <reified T> MessageBodyReadableContent.single(): Single<T> {
    return this.`as`(T::class.java)
}

fun <T> MessageBodyReadableContent.asSingle(type:Class<T> ): Single<T> {
    return this.`as`(type)
}

fun <T> DbRow.asType(klass:Class<T>):T{
 return this.`as`(klass)
}

fun <T> DbRow.asType(klass:GenericType<T>):T{
    return this.`as`(klass)
}

fun <T> DbRow.asType(klass:Function<DbRow,T>):T {
    return this.`as`(klass)
}


fun <T> Config.asType(type: GenericType<T>): ConfigValue<T>{
    return this.`as`(type);
}

fun <T> Config.asType(type: Class<T>): ConfigValue<T>{
    return this.`as`(type);
}

fun <T> Config.asType(type: Function<Config, T>): ConfigValue<T>{
    return this.`as`(type);
}

@Throws(MapperException::class)
fun <T> DbColumn.asType(type: Class<T>): T{
    return this.`as`(type)
}

@Throws(MapperException::class)
fun <T> DbColumn.asType(type: GenericType<T>): T{
    return this.`as`(type)
}

fun <T> ReadableBodyPart.asType(clazz:Class<T>):T {
    return this.`as`(clazz)
}
