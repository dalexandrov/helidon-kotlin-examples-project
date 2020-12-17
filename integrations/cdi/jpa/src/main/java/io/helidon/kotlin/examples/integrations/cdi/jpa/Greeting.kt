/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.examples.integrations.cdi.jpa

import java.util.*
import javax.persistence.*

/**
 * A contrived representation for example purposes only of a two-part
 * greeting as might be stored in a database.
 */
@Access(AccessType.FIELD)
@Entity(name = "Greeting")
@Table(name = "GREETING")
open class Greeting {
    @Id
    @Column(name = "FIRSTPART", insertable = true, nullable = false, updatable = false)
    private var firstPart: String? = null

    @Basic(optional = false)
    @Column(name = "SECONDPART", insertable = true, nullable = false, updatable = true)
    private var secondPart: String? = null

    /**
     * Creates a new [Greeting]; required by the JPA
     * specification and for no other purpose.
     *
     * @see .Greeting
     */
    @Deprecated("""Please use the {@link #Greeting(String,
     * String)} constructor instead.
     
      """)
    protected constructor() : super() {
    }

    /**
     * Creates a new [Greeting].
     *
     * @param firstPart the first part of the greeting; must not be
     * `null`
     *
     * @param secondPart the second part of the greeting; must not be
     * `null`
     *
     * @exception NullPointerException if `firstPart` or `secondPart` is `null`
     */
    constructor(firstPart: String, secondPart: String) : super() {
        this.firstPart = Objects.requireNonNull(firstPart)
        this.secondPart = Objects.requireNonNull(secondPart)
    }

    /**
     * Sets the second part of this greeting.
     *
     * @param secondPart the second part of this greeting; must not be
     * `null`
     *
     * @exception NullPointerException if `secondPart` is `null`
     */
    open fun setSecondPart(secondPart: String) {
        this.secondPart = Objects.requireNonNull(secondPart)
    }

    /**
     * Returns a [String] representation of the second part of
     * this [Greeting].
     *
     *
     * This method never returns `null`.
     *
     * @return a non-`null` [String] representation of the
     * second part of this [Greeting]
     */
    override fun toString(): String {
        return secondPart!!
    }
}