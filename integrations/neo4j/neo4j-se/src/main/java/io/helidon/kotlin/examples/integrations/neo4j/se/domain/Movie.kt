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
 *
 */
package io.helidon.kotlin.examples.integrations.neo4j.se.domain

import java.util.*

/**
 * The Movie class.
 */
class Movie
/**
 * Constructor for Movie.
 *
 * @param title
 * @param description
 */(val title: String, val description: String) {
    var actors: List<Actor> = ArrayList()
    var directors: List<Person> = ArrayList()
        private set
    var released: Int? = null
    fun setDirectorss(directors: List<Person>) {
        this.directors = directors
    }
}