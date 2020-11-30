/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.microprofile.example.helloworld.implicit

import io.helidon.config.Config
import io.helidon.kotlin.microprofile.example.helloworld.implicit.cdi.LoggerQualifier
import io.helidon.kotlin.microprofile.example.helloworld.implicit.cdi.RequestId
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.util.logging.Logger
import javax.enterprise.context.RequestScoped
import javax.enterprise.inject.spi.BeanManager
import javax.inject.Inject
import javax.json.Json
import javax.json.JsonObject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Resource for hello world example.
 */
@Path("helloworld")
@RequestScoped
open class HelloWorldResource
/**
 * Using constructor injection for field values.
 *
 * @param config      configuration instance
 * @param logger      logger (from [ResourceProducer]
 * @param requestId   requestId (from [ResourceProducer]
 * @param appName     name from configuration (app.name)
 * @param appUri      URI from configuration (app.uri)
 * @param beanManager bean manager (injected automatically by CDI)
 */ @Inject constructor(private val config: Config,
                        @param:LoggerQualifier private val logger: Logger,
                        @param:RequestId private val requestId: Int,
                        @param:ConfigProperty(name = "app.name") private val applicationName: String,
                        @param:ConfigProperty(name = "app.uri") private val applicationUri: URI,
                        private val beanManager: BeanManager) {
    /**
     * Get method for this resource, shows logger and request id.
     *
     * @return hello world
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    open fun message(): String {
        return "Hello World: $logger, request: $requestId, appName: $applicationName"
    }

    /**
     * Get method for this resource, returning JSON.
     *
     * @param name name to add to response
     * @return JSON structure with injected fields
     */
    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    open fun getHello(@PathParam("name") name: String?): JsonObject {
        return JSON.createObjectBuilder()
                .add("name", name)
                .add("requestId", requestId)
                .add("appName", applicationName)
                .add("appUri", applicationUri.toString())
                .add("config", config["server.port"].asInt().get())
                .add("beanManager", beanManager.toString())
                .add("logger", logger.name)
                .build()
    }

    companion object {
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())
    }
}