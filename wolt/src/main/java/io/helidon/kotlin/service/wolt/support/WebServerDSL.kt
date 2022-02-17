package support;

import io.helidon.common.pki.KeyConfig
import io.helidon.config.Config
import io.helidon.health.HealthSupport
import io.helidon.media.common.MediaContext
import io.helidon.security.Security
import io.helidon.webserver.*
import io.helidon.webserver.cors.CorsSupport

/**
 * DSL for the builder for WebServer and support objects.
 */
fun webServer(block: WebServer.Builder.() -> Unit = {}): WebServer = WebServer.builder().apply(block).build()

fun webServerTls(block: WebServerTls.Builder.() -> Unit = {}): WebServerTls = WebServerTls.builder().apply(block).build()

fun serverConfiguration(block: ServerConfiguration.Builder.() -> Unit = {}): ServerConfiguration =
    ServerConfiguration.builder().apply(block).build()

fun routing(block: Routing.Builder.() -> Unit = {}): Routing = Routing.builder().apply(block).build()

fun requestPredicate(block: RequestPredicate.() -> Unit = {}): RequestPredicate = RequestPredicate.create().apply(block)


fun mediaContext(block: MediaContext.Builder.() -> Unit = {}): MediaContext = MediaContext.builder().apply(block).build()

fun socketConfiguration(block: SocketConfiguration.Builder.() -> Unit = {}): SocketConfiguration =
    SocketConfiguration.builder().apply(block).build()

fun keystoreBuilder(block: KeyConfig.KeystoreBuilder.() -> Unit = {}): KeyConfig =
    KeyConfig.keystoreBuilder().apply(block).build()

fun healthSupport(block: HealthSupport.Builder.() -> Unit = {}): HealthSupport = HealthSupport.builder().apply(block).build()

fun corsSupport(block: CorsSupport.Builder.() -> Unit = {}): CorsSupport = CorsSupport.builder().apply(block).build()

fun security(block: Security.Builder.() -> Unit = {}): Security = Security.builder().apply(block).build()

fun config(block: Config.Builder.() -> Unit = {}): Config = Config.builder().apply(block).build()
