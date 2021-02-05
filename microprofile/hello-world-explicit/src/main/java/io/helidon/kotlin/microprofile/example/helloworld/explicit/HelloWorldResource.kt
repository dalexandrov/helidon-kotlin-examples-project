/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.microprofile.example.helloworld.explicit

import io.helidon.config.Config
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
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
 * Example resource.
 */
@Path("helloworld")
@RequestScoped
open class HelloWorldResource
/**
 * Constructor injection of field values.
 *
 * @param config      configuration instance
 * @param appName     name of application from config (app.name)
 * @param appUri      URI of application from config (app.uri)
 * @param beanManager bean manager (injected automatically by CDI)
 */ @Inject constructor(private val config: Config,
                        @param:ConfigProperty(name = "app.name") private val applicationName: String,
                        @param:ConfigProperty(name = "app.uri") private val applicationUri: URI,
                        private val beanManager: BeanManager) {
    /**
     * Hello world GET method.
     *
     * @return string with application name
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    open fun message(): String {
        return "Hello World from application $applicationName"
    }

    /**
     * Hello World GET method returning JSON.
     *
     * @param name name to add to response
     * @return JSON with name and configured fields of this class
     */
    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    open fun getHello(@PathParam("name") name: String?): JsonObject {
        return JSON.createObjectBuilder()
                .add("name", name)
                .add("appName", applicationName)
                .add("appUri", applicationUri.toString())
                .add("config", config["my.property"].asString().get())
                .add("beanManager", beanManager.toString())
                .build()
    }

    companion object {
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())
    }
}