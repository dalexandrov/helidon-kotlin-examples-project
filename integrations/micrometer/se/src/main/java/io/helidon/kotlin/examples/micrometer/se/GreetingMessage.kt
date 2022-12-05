/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.micrometer.se

import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonBuilderFactory

/**
 * POJO for the greeting message exchanged between the server and the client.
 */
class GreetingMessage
/**
 * Create a new greeting with the specified message content.
 *
 * @param message the message to store in the greeting
 */(
    /**
     * Sets the message value.
     *
     * @param message value to be set
     */
    var message: String
) {
    /**
     * Returns the message value.
     *
     * @return the message
     */

    /**
     * Prepares a [JsonObject] corresponding to this instance.
     *
     * @return `JsonObject` representing this `GreetingMessage` instance
     */
    fun forRest(): JsonObject {
        val builder = JSON_BF.createObjectBuilder()
        return builder.add(JSON_LABEL, message)
            .build()
    }

    companion object {
        /**
         * Label for tagging a `GreetingMessage` instance in JSON.
         */
        const val JSON_LABEL = "greeting"
        private val JSON_BF = Json.createBuilderFactory(emptyMap<String, Any>())

        /**
         * Converts a JSON object (typically read from the request payload)
         * into a `GreetingMessage`.
         *
         * @param jsonObject the [JsonObject] to convert.
         * @return `GreetingMessage` set according to the provided object
         */
        @JvmStatic
        fun fromRest(jsonObject: JsonObject): GreetingMessage {
            return GreetingMessage(jsonObject.getString(JSON_LABEL))
        }
    }
}