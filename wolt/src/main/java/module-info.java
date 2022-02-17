/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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


module io.helidon.kotlin.service.wolt {
    requires java.logging;

    requires io.helidon.dbclient;
    requires io.helidon.common.http;
    requires io.helidon.webserver;
    requires java.json;
    requires io.helidon.dbclient.health;
    requires io.helidon.health;
    requires io.helidon.media.jsonb;
    requires io.helidon.media.jsonp;
    requires io.helidon.metrics;
    requires io.helidon.integrations.vault;
    requires io.helidon.integrations.vault.sys;
    requires io.helidon.webclient;
    requires jakarta.websocket.api;
    requires io.helidon.messaging;
    requires io.helidon.microprofile.reactive;
    requires io.helidon.messaging.connectors.kafka;
    requires io.helidon.webserver.staticcontent;
    requires io.helidon.webserver.tyrus;
    requires io.helidon.integrations.vault.secrets.transit;
    requires kafka.clients;
    requires kotlin.stdlib;

    requires io.helidon.integrations.common.rest;
    requires io.helidon.servicecommon.rest;
    requires io.helidon.security;
    requires io.helidon.tracing;


    //provides io.helidon.dbclient.spi.DbMapperProvider with io.helidon.kotlin.service.wolt.DeliveryMapperProvider;
}
