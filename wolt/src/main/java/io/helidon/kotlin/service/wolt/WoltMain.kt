/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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

package io.helidon.kotlin.service.wolt;

import io.helidon.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.health.DbClientHealthCheck;
import io.helidon.health.HealthSupport;
import io.helidon.integrations.vault.Vault;
import io.helidon.integrations.vault.secrets.transit.TransitSecretsRx;
import io.helidon.integrations.vault.sys.SysRx;
import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.tracing.TracerBuilder;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.staticcontent.StaticContentSupport;
import io.helidon.webserver.tyrus.TyrusSupport;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import javax.websocket.server.ServerEndpointConfig;
import java.util.concurrent.TimeUnit;

public final class WoltMain {

    /**
     * Cannot be instantiated.
     */
    private WoltMain() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        startServer();
        //ReactiveStreams.of(5).ignore().run();
    }

    /**
     * Start the server.
     *
     * @return the created {@link WebServer} instance
     */
    static WebServer startServer() {

        // load logging configuration
        LogConfig.configureRuntime();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        // Prepare routing for the server
        WebServer server = WebServer.builder()
                .routing(createRouting(config))
                // Get webserver config from the "server" section of application.yaml
                .config(config.get("server"))
                .tracer(TracerBuilder.create(config.get("tracing")))
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonbSupport.create())
                .build();

        // Start the server and print some info.
        server.start().thenAccept(ws -> {
            System.out.println(
                    "WEB server is up! http://localhost:" + ws.port() + "/");
        });

        // Server threads are not daemon. NO need to block. Just react.
        server.whenShutdown().thenRun(() -> System.out.println("WEB server is DOWN. Good bye!"));

        return server;
    }

    /**
     * Creates new {@link Routing}.
     *
     * @param config configuration of this server
     * @return routing configured with JSON support, a health check, and a service
     */
    private static Routing createRouting(Config config) {
        Config dbConfig = config.get("db");

        // Client services are added through a service loader - see mongoDB example for explicit services
        DbClient dbClient = DbClient.builder(dbConfig)
                .build();

        // Initialize Vault Crypto services
        Vault tokenVault = Vault.builder()
                .config(config.get("vault.token"))
                .updateWebClient(it -> it.connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS))
                .build();
        SysRx sys = tokenVault.sys(SysRx.API);
        TransitSecretsRx secrets = tokenVault.secrets(TransitSecretsRx.ENGINE);


        CryptoServiceRx cryptoService = new CryptoServiceRx(sys, secrets);


        SendingServiceRx sendingService = new SendingServiceRx(config);


        // Some relational databases do not support DML statement as ping so using query which works for all of them
        HealthSupport health = HealthSupport.builder()
                .addLiveness(
                        DbClientHealthCheck.create(dbClient, dbConfig.get("health-check")))
                .build();

        return Routing.builder()
                .register(StaticContentSupport.builder("/WEB").welcomeFileName("index.html"))
                .register(health)                   // Health at "/health"
                .register(MetricsSupport.create())  // Metrics at "/metrics"
                .register("/db", new DeliveryService(dbClient, cryptoService, sendingService))
                .register("/ws",
                        TyrusSupport.builder().register(
                                        ServerEndpointConfig.Builder.create(
                                                        WebSocketEndpoint.class, "/messages")
                                                .build())
                                .build())
                .build();
    }
}
