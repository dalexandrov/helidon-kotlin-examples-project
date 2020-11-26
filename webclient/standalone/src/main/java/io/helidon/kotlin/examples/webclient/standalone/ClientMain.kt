/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.webclient.standalone

import io.helidon.common.http.Http
import io.helidon.config.Config
import io.helidon.media.common.MessageBodyReadableContent
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.RegistryFactory
import io.helidon.webclient.FileSubscriber
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientResponse
import io.helidon.webclient.metrics.WebClientMetrics
import io.helidon.webclient.spi.WebClientService
import org.eclipse.microprofile.metrics.MetricRegistry
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.function.Function
import javax.json.Json
import javax.json.JsonObject

/**
 * A simple WebClient usage class.
 *
 * Each of the methods demonstrates different usage of the WebClient.
 */
object ClientMain {
    private val METRIC_REGISTRY = RegistryFactory.getInstance()
            .getRegistry(MetricRegistry.Type.APPLICATION)
    private val JSON_BUILDER = Json.createBuilderFactory(emptyMap<String, Any>())
    private val JSON_NEW_GREETING: JsonObject?

    /**
     * Executes WebClient examples.
     *
     * If no argument provided it will take server port from configuration server.port.
     *
     * User can override port from configuration by main method parameter with the specific port.
     *
     * @param args main method
     * @throws ExecutionException   execution exception
     * @throws InterruptedException interrupted exception
     * @throws IOException          io exception
     */
    @Throws(ExecutionException::class, InterruptedException::class, IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Config.create()
        val url: String
        url = if (args.size == 0) {
            val port = config["server.port"].asInt()
            check(!(!port.isPresent || port.get() == -1)) {
                ("Unknown port! Please specify port as a main method parameter "
                        + "or directly to config server.port")
            }
            "http://localhost:" + port.get() + "/greet"
        } else {
            "http://localhost:" + args[0].toInt() + "/greet"
        }
        val webClient = WebClient.builder()
                .baseUri(url)
                .config(config["client"]) //Since JSON processing support is not present by default, we have to add it.
                .addMediaSupport(JsonpSupport.create())
                .build()
        performPutMethod(webClient)
                .thenCompose { it: Void? -> performGetMethod(webClient) }
                .thenCompose { it: String? -> followRedirects(webClient) }
                .thenCompose { it: String? -> getResponseAsAnJsonObject(webClient) }
                .thenCompose<Void?>(Function { it: JsonObject? -> saveResponseToFile(webClient) })
                .toCompletableFuture()
                .get()
    }

    @JvmStatic
    fun performPutMethod(webClient: WebClient): CompletionStage<Void> {
        println("Put request execution.")
        return webClient.put()
                .path("/greeting")
                .submit(JSON_NEW_GREETING)
                .thenAccept { webClientResponse: WebClientResponse? -> println("PUT request successfully executed.") }
    }

    @JvmStatic
    fun performGetMethod(webClient: WebClient): CompletionStage<String?> {
        println("Get request execution.")
        return webClient.get()
                .request(String::class.java)
                .thenCompose { string: String? ->
                    println("GET request successfully executed.")
                    println(string)
                    CompletableFuture.completedFuture(string)
                }
    }

    @JvmStatic
    fun followRedirects(webClient: WebClient): CompletionStage<String?> {
        println("Following request redirection.")
        return webClient.get()
                .path("/redirect")
                .request()
                .thenCompose { response: WebClientResponse ->
                    check(!(response.status() !== Http.Status.OK_200)) { "Follow redirection failed!" }
                    response.content().`as`(String::class.java)
                }
                .thenCompose { string: String? ->
                    println("Redirected request successfully followed.")
                    println(string)
                    CompletableFuture.completedFuture(string)
                }
    }

    fun getResponseAsAnJsonObject(webClient: WebClient): CompletionStage<JsonObject?> {
        //Support for JsonObject reading from response is not present by default.
        //In case of this example it was registered at creation time of the WebClient instance.
        println("Requesting from JsonObject.")
        return webClient.get()
                .request(JsonObject::class.java)
                .thenCompose { jsonObject: JsonObject? ->
                    println("JsonObject successfully obtained.")
                    println(jsonObject)
                    CompletableFuture.completedFuture(jsonObject)
                }
    }

    @JvmStatic
    fun saveResponseToFile(webClient: WebClient): CompletionStage<Void?> {
        //We have to create file subscriber first. This subscriber will save the content of the response to the file.
        val file = Paths.get("test.txt")
        try {
            Files.deleteIfExists(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val fileSubscriber = FileSubscriber.create(file)

        //Then it is needed obtain unhandled response content and subscribe file subscriber to it.
        println("Downloading server response to the file: $file")
        return webClient.get()
                .request()
                .thenApply { obj: WebClientResponse -> obj.content() }
                .thenCompose { publisher: MessageBodyReadableContent? -> fileSubscriber.subscribeTo(publisher) }
                .thenAccept { path: Path? -> println("Download complete!") }
    }

    init {
        JSON_NEW_GREETING = JSON_BUILDER.createObjectBuilder()
                .add("greeting", "Hola")
                .build()
    }
}