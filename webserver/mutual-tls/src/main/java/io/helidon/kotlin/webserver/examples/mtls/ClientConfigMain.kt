/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.mtls

import io.helidon.config.Config
import io.helidon.webclient.WebClient


/**
 * Start the example.
 * This example executes two requests by Helidon [WebClient] which are configured
 * by the configuration.
 *
 * You have to execute either [ServerBuilderMain] or [ServerConfigMain] for this to work.
 *
 * If any of the ports has been changed, you have to update ports in this main method also.
 *
 * @param args start arguments are ignored
 */
fun main(args: Array<String>) {
    val config = Config.create()
    val webClient = WebClient.create(config["client"])
    println("Contacting unsecured endpoint!")
    println("Response: " + callUnsecured(webClient, 8080))
    println("Contacting secured endpoint!")
    println("Response: " + callSecured(webClient, 443))
}

fun callUnsecuredConfig(webClient: WebClient, port: Int): String {
    return webClient.get()
        .uri("http://localhost:$port")
        .request(String::class.java)
        .await()
}

fun callSecuredConfig(webClient: WebClient, port: Int): String {
    return webClient.get()
        .uri("https://localhost:$port")
        .request(String::class.java)
        .await()
}