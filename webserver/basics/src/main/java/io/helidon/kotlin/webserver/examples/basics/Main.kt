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
package io.helidon.kotlin.webserver.examples.basics

import asSingle
import io.helidon.common.http.Http
import io.helidon.common.http.MediaType
import io.helidon.media.common.MediaContext
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webserver.*
import io.helidon.webserver.jersey.JerseySupport
import jerseySupport
import mediaContext
import requestPredicate
import routing
import webServer
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import javax.json.Json
import kotlin.system.exitProcess

/**
 * This example consists of few first tutorial steps of WebServer API. Each step is represented by a single method.
 *
 *
 * **Principles:**
 *
 *  * Reactive principles
 *  * Reflection free
 *  * Fluent
 *  * Integration platform
 *
 *
 *
 * It is also java executable main class. Use a method name as a command line parameter to execute.
 */
open class Main {
    // ---------------- EXAMPLES
    /**
     * True heart of WebServer API is [Routing]. It provides fluent way how to assign custom [Handler] to the routing
     * rule. The rule consists from two main factors - *HTTP method* and *path pattern*.
     *
     *
     * The (route) [Handler] is a functional interface which process HTTP [request][io.helidon.webserver.ServerRequest] and
     * writes to the [response][io.helidon.webserver.ServerResponse].
     */
    fun firstRouting() {
        val routing = routing {
            post("/post-endpoint", Handler { _: ServerRequest?, res: ServerResponse ->
                res.status(Http.Status.CREATED_201)
                    .send()
            })
            get("/get-endpoint", Handler { _: ServerRequest?, res: ServerResponse ->
                res.status(Http.Status.NO_CONTENT_204)
                    .send("Hello World!")
            })
        }
        startServer(routing)
    }

    /**
     * [Routing] instance can be used to create [WebServer] instance.
     * It provides a simple, non-blocking life-cycle API returning
     * [CompletionStages][java.util.concurrent.CompletionStage] to provide reactive access.
     *
     * @param routing the routing to drive by WebServer instance
     * @param mediaContext media support
     */
    protected open fun startServer(routing: Routing, mediaContext: MediaContext?) {
        webServer {
            routing(routing)
            mediaContext(mediaContext)
        }.start() // All lifecycle operations are non-blocking and provides CompletionStage
            .whenComplete { ws: WebServer, thr: Throwable? ->
                if (thr == null) {
                    println("Server is UP: http://localhost:" + ws.port())
                } else {
                    println("Can NOT start WebServer!")
                    thr.printStackTrace(System.out)
                }
            }
    }

    /**
     * [Routing]
     * can be used to create [WebServer] instance.It provides a simple, non-blocking life-cycle API returning
     * [CompletionStages][java.util.concurrent.CompletionStage] to provide reactive access.
     *
     * @param routing the routing to drive by WebServer instance
     */
    private fun startServer(routing: Routing) {
        startServer(routing, null)
    }

    /**
     * All routing rules (routes) are evaluated in a definition order. The [Handler] assigned with the first valid route
     * for given request is called. It is a responsibility of each handler to process in one of the following ways:
     *
     *  * Respond using one of [ServerResponse.send(...)][io.helidon.webserver.ServerResponse.send] method.
     *  * Continue to next valid route using [ServerRequest.next()][io.helidon.webserver.ServerRequest.next] method.
     * *It is possible to define filtering handlers.*
     *
     *
     *
     * If no valid [Handler] is found then routing respond by `HTTP 404` code.
     *
     *
     * If selected [Handler] doesn't process request than the request **stacks**!
     *
     *
     * **Blocking operations:**<br></br>
     * For performance reason, [Handler] can be called directly by a selector thread. It is not good idea to block
     * such thread. If request must be processed by a blocking operation then such processing should be deferred to another
     * thread.
     */
    fun routingAsFilter() {
        val routing = routing {
            any(Handler { req: ServerRequest, _: ServerResponse? ->
                println(req.method().toString() + " " + req.path())
                // Filters are just routing handlers which calls next()
                req.next()
            })
            post("/post-endpoint", Handler { _: ServerRequest?, res: ServerResponse ->
                res.status(Http.Status.CREATED_201)
                    .send()
            })["/get-endpoint", Handler { _: ServerRequest?, res: ServerResponse ->
                res.status(Http.Status.NO_CONTENT_204)
                    .send("Hello World!")
            }]
        }
        startServer(routing)
    }

    /**
     * [ServerRequest][io.helidon.webserver.ServerRequest] provides access to three types of "parameters":
     *
     *  * Headers
     *  * Query parameters
     *  * Path parameters - *Evaluated from provided `path pattern`*
     *
     *
     *
     * [Optional][java.util.Optional] API is heavily used to represent parameters optionality.
     *
     *
     * WebServer [Parameters] API is used to represent fact, that *headers* and
     * *query parameters* can contain multiple values.
     */
    fun parametersAndHeaders() {
        val routing = routing {
            get("/context/{id}", Handler { req: ServerRequest, res: ServerResponse ->
                val sb = StringBuilder()
                // Request headers
                req.headers()
                    .first("foo")
                    .ifPresent { v: String? -> sb.append("foo: ").append(v).append("\n") }
                // Request parameters
                req.queryParams()
                    .first("bar")
                    .ifPresent { v: String? -> sb.append("bar: ").append(v).append("\n") }
                // Path parameters
                sb.append("id: ").append(req.path().param("id"))
                // Response headers
                res.headers().contentType(MediaType.TEXT_PLAIN)
                // Response entity (payload)
                res.send(sb.toString())
            })
        }
        startServer(routing)
    }

    /**
     * Routing rules (routes) are limited on two criteria - *HTTP method and path*. [RequestPredicate] can be used
     * to specify more complex criteria.
     */
    fun advancedRouting() {
        val routing = routing {
            get("/foo", requestPredicate {
                accepts(MediaType.TEXT_PLAIN)
                containsHeader("bar")
            }
                .thenApply { _: ServerRequest, res: ServerResponse ->
                    res.send()
                })
        }
        startServer(routing)
    }

    /**
     * Larger applications with many routing rules can cause complicated readability (maintainability) if all rules are
     * defined in a single fluent code. It is possible to register [Service][io.helidon.webserver.Service] and organise
     * the code into services and resources. `Service` is an interface which can register more routing rules (routes).
     */
    fun organiseCode() {
        val routing = routing {
            register("/catalog-context-path", Catalog())
        }
        startServer(routing)
    }

    /**
     * Request payload (body/entity) is represented by [Flow.Publisher][java.util.concurrent.Flow.Publisher]
     * of [RequestChunks][DataChunk] to enable reactive processing of the content of any size.
     * But it is more convenient to process entity in some type specific form. WebServer supports few types which can be
     * used te read the whole entity:
     *
     *  * `byte[]`
     *  * `String`
     *  * `InputStream`
     *
     *
     *
     * Similar approach is used for the response entity.
     */
    fun readContentEntity() {
        val routing = Routing.builder()
            .post("/foo", Handler { req: ServerRequest, res: ServerResponse ->
                req.content()
                    .asSingle(String::class.java) // The whole entity can be read when all request chunks are processed - CompletionStage
                    .whenComplete { data: String, thr: Throwable? ->
                        if (thr == null) {
                            println("/foo DATA: $data")
                            res.send(data)
                        } else {
                            res.status(Http.Status.BAD_REQUEST_400)
                        }
                    }
            }) // It is possible to use Handler.of() method to automatically cover all error states.
            .post(
                "/bar",
                Handler.create(String::class.java) { _: ServerRequest?, res: ServerResponse, data: String ->
                    println("/foo DATA: $data")
                    res.send(data)
                })
            .build()
        startServer(routing)
    }

    /**
     * Use a custom [reader][MessageBodyReader] to convert the request content into an object of a given type.
     */
    fun mediaReader() {
        val routing = Routing.builder()
            .post(
                "/create-record",
                Handler.create(Name::class.java) { _: ServerRequest?, res: ServerResponse, name: Name ->
                    println("Name: $name")
                    res.status(Http.Status.CREATED_201)
                        .send(name.toString())
                })
            .build()

        // Create a media support that contains the defaults and our custom Name reader
        val mediaContext = MediaContext.builder()
            .addReader(NameReader.create())
            .build()
        startServer(routing, mediaContext)
    }

    /**
     * Combination of filtering [Handler] pattern with [Service][io.helidon.webserver.Service] registration capabilities
     * can be used by other frameworks for the integration. WebServer is shipped with several integrated libraries (supports)
     * including *static content*, JSON and Jersey. See `POM.xml` for requested dependencies.
     */
    fun supports() {
        val routing = routing {
            register(StaticContentSupport.create("/static"))
            get("/hello/{what}", Handler { req: ServerRequest, res: ServerResponse ->
                res.send(
                    JSON.createObjectBuilder()
                        .add(
                            "message",
                            "Hello " + req.path()
                                .param("what")
                        )
                        .build()
                )
            })
            register(
                "/api", jerseySupport {
                    register(HelloWorldResource::class.java)
                }
            )
        }
        val mediaContext = mediaContext {
            addWriter(JsonpSupport.writer())
        }
        startServer(routing, mediaContext)
    }

    /**
     * Request processing can cause error represented by [Throwable]. It is possible to register custom
     * [ErrorHandlers][io.helidon.webserver.ErrorHandler] for specific processing.
     *
     *
     * If error is not processed by a custom [ErrorHandler][io.helidon.webserver.ErrorHandler] than default one is used.
     * It respond with *HTTP 500 code* unless error is not represented
     * by [HttpException]. In such case it reflects its content.
     */
    fun errorHandling() {
        val routing = routing {
            post(
                "/compute",
                Handler.create(String::class.java) { _: ServerRequest?, res: ServerResponse, str: String ->
                    val result = 100 / str.toInt()
                    res.send("100 / $str = $result")
                })
            error(Throwable::class.java) { req: ServerRequest, _: ServerResponse?, ex: Throwable ->
                ex.printStackTrace(System.out)
                req.next()
            }
            error(
                NumberFormatException::class.java
            ) { _: ServerRequest?, res: ServerResponse, _: NumberFormatException? ->
                res.status(Http.Status.BAD_REQUEST_400).send()
            }
            error(
                ArithmeticException::class.java
            ) { _: ServerRequest?, res: ServerResponse, _: ArithmeticException? ->
                res.status(Http.Status.PRECONDITION_FAILED_412).send()
            }
        }
        startServer(routing)
    }

    /**
     * Prints usage instructions.
     */
    private fun help() {
        val hlp = StringBuilder()
        hlp.append("java -jar example-basics.jar <exampleMethodName>\n")
        hlp.append("Example method names:\n")
        val methods = Main::class.java.declaredMethods
        for (method in methods) {
            if (!Modifier.isPrivate(method.modifiers) && !Modifier.isStatic(method.modifiers)) {
                hlp.append("    ").append(method.name).append('\n')
            }
        }
        hlp.append('\n')
        hlp.append("Example method name can be also provided as a\n")
        hlp.append("    - -D").append(SYSPROP_EXAMPLE_NAME).append(" jvm property.\n")
        hlp.append("    - ").append(ENVVAR_EXAMPLE_NAME).append(" environment variable.\n")
        println(hlp)
    }

    /**
     * Prints usage instructions. (Shortcut to [.help] method.
     */
    fun h() {
        help()
    }

    companion object {
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())

        // ---------------- EXECUTION
        private const val SYSPROP_EXAMPLE_NAME = "exampleName"
        private const val ENVVAR_EXAMPLE_NAME = "EXAMPLE_NAME"

        /**
         * Java main method.
         *
         * @param args Command line arguments.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            var exampleName: String? = null
            exampleName = when {
                args.isNotEmpty() -> {
                    args[0]
                }
                System.getProperty(SYSPROP_EXAMPLE_NAME) != null -> {
                    System.getProperty(SYSPROP_EXAMPLE_NAME)
                }
                System.getenv(ENVVAR_EXAMPLE_NAME) != null -> {
                    System.getenv(ENVVAR_EXAMPLE_NAME)
                }
                else -> {
                    println(
                        """Missing example name. It can be provided as a 
            - first command line argument.
            - -D$SYSPROP_EXAMPLE_NAME jvm property.
            - $ENVVAR_EXAMPLE_NAME environment variable."""
                    )
                    exitProcess(1)
                }
            }
            while (exampleName!!.startsWith("-")) {
                exampleName = exampleName.substring(1)
            }
            val m = Main()
            try {
                val method = Main::class.java.getMethod(exampleName)
                method.invoke(m)
            } catch (e: NoSuchMethodException) {
                println("Missing example method named: $exampleName")
                exitProcess(2)
            } catch (e: IllegalAccessException) {
                e.printStackTrace(System.out)
                exitProcess(100)
            } catch (e: InvocationTargetException) {
                e.printStackTrace(System.out)
                exitProcess(100)
            }
        }
    }
}