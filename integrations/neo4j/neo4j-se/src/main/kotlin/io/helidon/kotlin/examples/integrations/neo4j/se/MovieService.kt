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
package io.helidon.kotlin.examples.integrations.neo4j.se

import io.helidon.kotlin.examples.integrations.neo4j.se.domain.MovieRepository
import io.helidon.webserver.*

/**
 * The Movie service.
 *
 * Implements [io.helidon.webserver.Service]
 */
class MovieService
/**
 * The movies service.
 * @param movieRepository
 */(private val movieRepository: MovieRepository) : Service {
    /**
     * Main routing done here.
     *
     * @param rules
     */
    override fun update(rules: Routing.Rules) {
        rules["/api/movies", Handler { request: ServerRequest, response: ServerResponse -> findMoviesHandler(response) }]
    }

    private fun findMoviesHandler(response: ServerResponse) {
        response.send(movieRepository.findAll())
    }
}