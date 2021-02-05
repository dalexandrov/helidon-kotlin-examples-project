/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.translator.frontend

import io.helidon.tracing.jersey.client.ClientTracingFilter
import io.helidon.webserver.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response

/**
 * Translator frontend resource.
 */
class TranslatorFrontendService internal constructor(backendHostname: String, backendPort: Int) : Service {
    private val backendTarget: WebTarget
    override fun update(rules: Routing.Rules) {
        rules[Handler { request: ServerRequest, response: ServerResponse -> getText(request, response) }]
    }

    private fun getText(request: ServerRequest, response: ServerResponse) {
        try {
            val query = request.queryParams().first("q")
                    .orElseThrow { BadRequestException("missing query parameter 'q'") }
            val language = request.queryParams().first("lang")
                    .orElseThrow { BadRequestException("missing query parameter 'lang'") }
            val backendResponse = backendTarget
                    .property(ClientTracingFilter.TRACER_PROPERTY_NAME, request.tracer())
                    .property(ClientTracingFilter.CURRENT_SPAN_CONTEXT_PROPERTY_NAME, request.spanContext().orElse(null))
                    .queryParam("q", query)
                    .queryParam("lang", language)
                    .request()
                    .get()
            val result: String
            result = if (backendResponse.statusInfo.family == Response.Status.Family.SUCCESSFUL) {
                backendResponse.readEntity(String::class.java)
            } else {
                "Error: " + backendResponse.readEntity(String::class.java)
            }
            response.send("""
    $result
    
    """.trimIndent())
        } catch (pe: ProcessingException) {
            LOGGER.log(Level.WARNING, "Problem to call translator frontend.", pe)
            response.status(503).send("Translator backend service isn't available.")
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(TranslatorFrontendService::class.java.name)
    }

    init {
        backendTarget = ClientBuilder.newClient()
                .target("http://$backendHostname:$backendPort")
    }
}