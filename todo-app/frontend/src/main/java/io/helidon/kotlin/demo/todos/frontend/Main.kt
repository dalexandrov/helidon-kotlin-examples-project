/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.kotlin.demo.todos.frontend

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.config.FileSystemWatcher
import io.helidon.config.PollingStrategies
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.MetricsSupport
import io.helidon.security.Security
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.tracing.TracerBuilder
import io.helidon.webserver.Routing
import io.helidon.webserver.StaticContentSupport
import io.helidon.webserver.WebServer
import io.helidon.webserver.accesslog.AccessLogSupport
import io.opentracing.Tracer
import org.glassfish.jersey.logging.LoggingFeature
import java.time.Duration
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import javax.ws.rs.client.ClientBuilder
import kotlin.system.exitProcess

/**
 * Main class to start the service.
 */
class Main

private const val POLLING_INTERVAL = 5L

/**
 * Application main entry point.
 * configuration
 */

fun main() {

    // load logging configuration
    LogManager.getLogManager().readConfiguration(
        Main::class.java.getResourceAsStream("/logging.properties")
    )

    // needed for default connection of Jersey client
    // to allow our headers to be set
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    val config = buildConfig()

    // build a client (Jersey)
    // and apply security and tracing features on it
    val client = ClientBuilder.newClient()
    client.register(LoggingFeature(Logger.getGlobal(), Level.FINE, LoggingFeature.Verbosity.PAYLOAD_ANY, 8192))
    val bsc = BackendServiceClient(client, config)

    // create a web server
    val server = WebServer.builder(
        createRouting(
            Security.create(config["security"]),
            config,
            bsc
        )
    )
        .config(config["webserver"])
        .addMediaSupport(JsonpSupport.create())
        .tracer(registerTracer(config))
        .build()

    // start the web server
    server.start()
        .whenComplete { webServer: WebServer, throwable: Throwable? -> started(webServer, throwable) }
}

/**
 * Create a `Tracer` instance using the given `Config`.
 * @param config the configuration root
 * @return the created `Tracer`
 */
private fun registerTracer(config: Config): Tracer {
    return TracerBuilder.create(config["tracing"]).build()
}

/**
 * Create the web server routing and register all handlers.
 * @param security the security features
 * @param config the configuration root
 * @param bsc the backend service client to use
 * @return the created `Routing`
 */
private fun createRouting(
    security: Security,
    config: Config,
    bsc: BackendServiceClient
): Routing {
    return Routing.builder()
        .register(AccessLogSupport.create()) // register metrics features (on "/metrics")
        .register(MetricsSupport.create()) // register security features
        .register(WebSecurity.create(security, config)) // register static content support (on "/")
        .register(
            StaticContentSupport.builder("/WEB").welcomeFileName("index.html")
        ) // register API handler (on "/api") - this path is secured (see application.yaml)
        .register("/api", TodosHandler(bsc)) // and a simple environment handler to see where we are
        .register("/env", EnvHandler(config))
        .build()
}

/**
 * Handle web server started event: if successful print server started
 * message in the console with the corresponding URL, otherwise print an
 * error message and exit the application.
 * @param webServer the `WebServer` instance
 * @param throwable if non `null`, indicate a server startup error
 */
private fun started(
    webServer: WebServer,
    throwable: Throwable?
) {
    if (throwable == null) {
        println("WEB server is up! http://localhost:" + webServer.port())
    } else {
        throwable.printStackTrace(System.out)
        exitProcess(1)
    }
}

/**
 * Load the configuration from all sources.
 * @return the configuration root
 */
private fun buildConfig(): Config {
    return Config.builder()
        .sources(
            listOf(
                ConfigSources.environmentVariables(),  // expected on development machine
                // to override props for dev
                ConfigSources.file("dev.yaml")
                    .changeWatcher(FileSystemWatcher.create())
                    .optional(),  // expected in k8s runtime
                // to configure testing/production values
                ConfigSources.file("prod.yaml")
                    .pollingStrategy(
                        PollingStrategies.regular(
                            Duration.ofSeconds(POLLING_INTERVAL)
                        )
                    )
                    .optional(),  // in jar file
                // (see src/main/resources/application.yaml)
                ConfigSources.classpath("application.yaml")
            )
        )
        .build()
}
