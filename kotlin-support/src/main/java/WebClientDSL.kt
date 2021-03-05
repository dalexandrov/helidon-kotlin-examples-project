import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webclient.WebClient


/**
 * DSL for the builder for WebClient.
 */
fun webClient(block: WebClient.Builder.() -> Unit): WebClient = WebClient.builder().apply(block).build()

fun jsonpSupport(block: JsonpSupport.Builder.() -> Unit): JsonpSupport = JsonpSupport.builder().apply(block).build();