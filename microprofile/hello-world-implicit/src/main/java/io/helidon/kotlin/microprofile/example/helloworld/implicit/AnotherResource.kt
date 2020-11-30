/*
 * Copyright (c) 2018,2020 Oracle and/or its affiliates. All rights reserved.
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

import org.eclipse.microprofile.config.Config
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.util.*
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.inject.Provider
import javax.ws.rs.GET
import javax.ws.rs.Path

/**
 * Resource showing all possible configuration injections.
 */
@Path("another")
@RequestScoped
open class AnotherResource {
    @Inject
    @ConfigProperty(name = "app.nonExistent", defaultValue = "145")
    private  var defaultValue = 0

    @Inject
    @ConfigProperty(name = "app.nonExistent")
    private  var empty: Optional<String>? = null

    @Inject
    @ConfigProperty(name = "app.uri")
    private  var full: Optional<URI>? = null

    @Inject
    @ConfigProperty(name = "app.someInt")
    private  var provider: Provider<Int>? = null

    @Inject
    @ConfigProperty(name = "app.ints")
    private var ints: List<Int>? = null

    @Inject
    @ConfigProperty(name = "app.ints")
    private var optionalInts: Optional<List<Int>>? = null

    @Inject
    @ConfigProperty(name = "app.ints")
    private var providedInts: Provider<List<Int>>? = null

    @Inject
    @ConfigProperty(name = "app.ints")
    private lateinit var intsArray: IntArray

    @Inject
    @ConfigProperty(name = "app")
    private var detached: Map<String, String>? = null

    @Inject
    private var mpConfig: Config? = null

    @Inject
    private var helidonConfig: io.helidon.config.Config? = null

    /**
     * Get method to validate that all injections worked.
     *
     * @return data from all fields of this class
     */
    @GET
    open fun get(): String {
        return toString()
    }

    override fun toString(): String {
        return ("AnotherResource{"
                + "defaultValue=" + defaultValue
                + ", empty=" + empty
                + ", full=" + full
                + ", provider=" + provider + "(" + provider!!.get() + ")"
                + ", ints=" + ints
                + ", optionalInts=" + optionalInts
                + ", providedInts=" + providedInts + "(" + providedInts!!.get() + ")"
                + ", detached=" + detached
                + ", microprofileConfig=" + mpConfig
                + ", helidonConfig=" + helidonConfig
                + ", intsArray=" + Arrays.toString(intsArray)
                + '}')
    }
}