/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.microprofile.example.tls

import javax.enterprise.context.RequestScoped
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * A simple JAX-RS resource to greet you. Examples:
 *
 * Get default greeting message:
 * curl -X GET https://localhost:8080
 *
 * The message is returned as a plain text.
 */
@Path("/")
@RequestScoped
open class GreetResource {
    /**
     * Return a greeting message.
     *
     * @return [JsonObject]
     */
    @get:Produces(MediaType.TEXT_PLAIN)
    @get:GET
    open val defaultMessage: String
        get() = "Hello user!"
}