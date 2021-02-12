import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webclient.WebClient



fun webClient(block: WebClient.Builder.() -> Unit): WebClient = WebClient.builder().apply(block).build()

fun jsonpSupport(block: JsonpSupport.Builder.() -> Unit): JsonpSupport = JsonpSupport.builder().apply(block).build();