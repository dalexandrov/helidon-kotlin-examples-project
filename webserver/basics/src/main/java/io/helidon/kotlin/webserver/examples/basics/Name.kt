/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Represents a simple entity - the name.
 */
class Name(fullName: String) {
    private var firstName: String? = null
    private var middleName: String? = null
    private var lastName: String? = null
    override fun toString(): String {
        val result = StringBuilder()
        if (firstName != null) {
            result.append(firstName)
        }
        if (middleName != null) {
            if (result.isNotEmpty()) {
                result.append(' ')
            }
            result.append(middleName)
        }
        if (lastName != null) {
            if (result.isNotEmpty()) {
                result.append(' ')
            }
            result.append(lastName)
        }
        return result.toString()
    }

    /**
     * An naive implementation of name parser.
     *
     */
    init {
        val split = fullName.split(" ").toTypedArray()
        when (split.size) {
            0 -> throw IllegalArgumentException("An empty name")
            1 -> {
                firstName = null
                middleName = null
                lastName = split[0]
            }
            2 -> {
                firstName = split[0]
                middleName = null
                lastName = split[1]
            }
            3 -> {
                firstName = split[0]
                middleName = split[1]
                lastName = split[2]
            }
            else -> throw IllegalArgumentException("To many name parts!")
        }
    }
}