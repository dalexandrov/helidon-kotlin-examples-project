/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.demo.todos.backend

import com.datastax.driver.core.Row
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.json.Json
import javax.json.JsonObject

/**
 * Data object for backend.
 */
class Todo {
    /**
     * Get the TODO ID.
     * @return the `String` identifying this entry
     */
    /**
     * The TODO ID.
     */
    var id: String? = null
        private set
    /**
     * Get the user ID associated with this TODO.
     * @return the `String` identifying the user
     */
    /**
     * The user ID associated with this TODO.
     */
    var userId: String? = null
        private set
    /**
     * Get the TODO title.
     * @return title
     */
    /**
     * The TODO title.
     */
    var title: String? = null
        private set
    /**
     * Get the completed flag.
     * @return completed flag.
     */
    /**
     * The TODO completed flag.
     */
    var completed: Boolean? = null
        private set
    /**
     * Get the creation timestamp.
     * @return timestamp
     */
    /**
     * The TODO creation timestamp.
     */
    var created: Long = 0
        private set

    /**
     * Convert this `Todo` instance to the JSON database format.
     * @return `JsonObject`
     */
    fun forDb(): JsonObject {
        //to store to DB
        val builder = JSON.createObjectBuilder()
        return builder.add("id", id)
                .add("user", userId)
                .add("message", title)
                .add("completed", completed!!)
                .add("created", created)
                .build()
    }

    /**
     * Convert this `Todo` instance to the JSON REST format.
     * @return `JsonObject`
     */
    fun forRest(): JsonObject {
        //to send over to rest
        val builder = JSON.createObjectBuilder()
        return builder.add("id", id)
                .add("user", userId)
                .add("title", title)
                .add("completed", completed!!)
                .add("created", created)
                .build()
    }

    /**
     * Set the completed flag.
     * @param iscomplete the completed flag value
     */
    fun setCompleted(iscomplete: Boolean) {
        completed = iscomplete
    }

    override fun toString(): String {
        return ("Todo{"
                + "id='" + id + '\''
                + ", userId='" + userId + '\''
                + ", title='" + title + '\''
                + ", completed=" + completed
                + ", created=" + created
                + '}')
    }

    companion object {
        /**
         * Date formatter to format the dates of the TODO entries.
         */
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSVV")

        /**
         * Factory for creating JSON builders.
         */
        private val JSON = Json.createBuilderFactory(emptyMap<String, Any>())

        /**
         * Create a new `Todo` instance from a database entry in JSON format.
         * @param jsonObject the database entry
         * @return the created instance
         */
        fun fromDb(jsonObject: JsonObject): Todo {
            val result = Todo()
            result.id = jsonObject.getString("id")
            result.userId = jsonObject.getString("user")
            result.title = jsonObject.getString("message")
            result.completed = jsonObject.getBoolean("completed")
            result.created = Instant.from(DATE_FORMAT
                    .parse(jsonObject.getString("created"))).toEpochMilli()
            return result
        }

        /**
         * Create a new `Todo` instance from a REST entry.
         * The created entry will be new, i.e the `completed` flag will be set
         * to `false` and the `created` timestamp set to the current
         * time.
         * @param jsonObject the REST entry
         * @param userId the user ID associated with this entry
         * @param id the entry ID
         * @return the created instance
         */
        fun newTodoFromRest(jsonObject: JsonObject,
                            userId: String?,
                            id: String?): Todo {
            val result = Todo()
            result.id = id
            result.userId = userId
            result.title = jsonObject.getString("title")
            result.completed = jsonObject.getBoolean("completed", false)
            result.created = System.currentTimeMillis()
            return result
        }

        /**
         * Create a new `Todo` instance from a REST entry.
         * @param jsonObject the REST entry
         * @param userId the user ID associated with this entry
         * @param id the entry ID
         * @return the created instance
         */
        fun fromRest(jsonObject: JsonObject,
                     userId: String?,
                     id: String?): Todo {
            val result = Todo()
            result.id = id
            result.userId = userId
            result.title = jsonObject.getString("title", "")
            result.completed = jsonObject.getBoolean("completed")
            val created = jsonObject.getJsonNumber("created")
            if (null != created) {
                result.created = created.longValue()
            }
            return result
        }

        /**
         * Create a new `Todo` instance from a database entry.
         * @param row the database entry
         * @return the created instance
         */
        fun fromDb(row: Row): Todo {
            val result = Todo()
            result.id = row.getString("id")
            result.userId = row.getString("user")
            result.title = row.getString("message")
            result.completed = row.getBool("completed")
            result.created = row.getTimestamp("created").time
            return result
        }

        /**
         * Create a new `Todo` instance.
         * The created entry will be new, i.e the `completed` flag will be set
         * to `false` and the `created` timestamp set to the current
         * time.
         * @param userId the user ID associated with the new entry
         * @param title the title for the new entry
         * @return the created instance
         */
        fun create(userId: String?, title: String?): Todo {
            val result = Todo()
            result.id = UUID.randomUUID().toString()
            result.userId = userId
            result.title = title
            result.completed = false
            result.created = System.currentTimeMillis()
            return result
        }
    }
}