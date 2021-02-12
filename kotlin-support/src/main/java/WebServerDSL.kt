import io.helidon.common.pki.KeyConfig
import io.helidon.media.common.MediaContext
import io.helidon.webserver.*
import io.helidon.webserver.jersey.JerseySupport

fun webServer(block: WebServer.Builder.() -> Unit): WebServer = WebServer.builder().apply(block).build()

fun webServerTls(block: WebServerTls.Builder.() -> Unit): WebServerTls = WebServerTls.builder().apply(block).build()

fun routing(block:Routing.Builder.() -> Unit):Routing = Routing.builder().apply(block).build()

fun requestPredicate(block:RequestPredicate.() -> Unit):RequestPredicate = RequestPredicate.create().apply(block);

fun jerseySupport(block: JerseySupport.Builder.() -> Unit):JerseySupport = JerseySupport.builder().apply(block).build();

fun mediaContext(block: MediaContext.Builder.() -> Unit):MediaContext = MediaContext.builder().apply(block).build();

fun socketConfiguration(block: SocketConfiguration.Builder.() -> Unit):SocketConfiguration = SocketConfiguration.builder().apply(block).build()

fun keystoreBuilder(block: KeyConfig.KeystoreBuilder.() -> Unit): KeyConfig = KeyConfig.keystoreBuilder().apply(block).build()

