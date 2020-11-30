/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import io.helidon.config.Config
import io.helidon.security.SecurityException
import io.opentracing.Span
import io.opentracing.SpanContext
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer
import java.util.*
import java.util.Map
import java.util.function.Consumer
import java.util.function.Supplier
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.collections.MutableList

/**
 * A service showing to access a no-SQL database.
 */
@ApplicationScoped
class DbService @Inject constructor(config: Config) {
    /**
     * The database session.
     */
    private val session: Session

    /**
     * The database statement for retrieving all entries.
     */
    private val listStatement: PreparedStatement

    /**
     * The database statement for retrieving a single entry.
     */
    private val getStatement: PreparedStatement

    /**
     * The database statement for inserting a new entry.
     */
    private val insertStatement: PreparedStatement

    /**
     * The database statement for updating an existing entry.
     */
    private val updateStatement: PreparedStatement

    /**
     * The database statement for deleting an entry.
     */
    private val deleteStatement: PreparedStatement

    /**
     * The database cluster.
     */
    private val cluster: Cluster

    /**
     * Retrieve the TODOs entries from the database.
     * @param tracingSpan the tracing span to use
     * @param userId the database user id
     * @return retrieved entries as `Iterable`
     */
    fun list(tracingSpan: SpanContext,
             userId: String?): Iterable<Todo> {
        return execute<List<Todo>>(tracingSpan, "cassandra::list") {
            val bs = listStatement.bind(userId)
            val rs = session.execute(bs)
            val result: MutableList<Todo> = ArrayList()
            for (r in rs) {
                result.add(Todo.fromDb(r))
            }
            result
        }
    }

    /**
     * Get the TODO entry identified by the given ID from the database.
     * @param tracingSpan the tracing span to use
     * @param id the ID identifying the entry to retrieve
     * @param userId the database user id
     * @return retrieved entry as `Optional`
     */
    operator fun get(tracingSpan: SpanContext,
                     id: String,
                     userId: String): Optional<Todo> {
        return execute(tracingSpan, "cassandra::get"
        ) { getNoContext(id, userId) }
    }

    /**
     * Get the TODO identified by the given ID from the database, fails if the
     * entry is not associated with the given `userId`.
     * @param id the ID identifying the entry to retrieve
     * @param userId the database user id
     * @return retrieved entry as `Optional`
     */
    private fun getNoContext(id: String,
                             userId: String): Optional<Todo> {
        val bs = getStatement.bind(id)
        val rs = session.execute(bs)
        val one = rs.one() ?: return Optional.empty()
        val result = Todo.fromDb(one)
        if (userId == result.userId) {
            return Optional.of(result)
        }
        throw SecurityException("User " + userId
                + " attempted to read record "
                + id + " of another user")
    }

    /**
     * Update the given TODO entry in the database.
     * @param tracingSpan the tracing span to use
     * @param todo the entry to update
     * @return `Optional` of updated entry if the update was successful,
     * otherwise an empty `Optional`
     */
    fun update(tracingSpan: SpanContext, todo: Todo): Optional<Todo> {
        return execute(tracingSpan, "cassandra::update") {

            //update backend set message = ?
            // , completed = ? where id = ? if user = ?
            val bs = updateStatement.bind(
                    todo.title,
                    todo.completed,
                    todo.id,
                    todo.userId)
            val execute = session.execute(bs)
            if (execute.wasApplied()) {
                return@execute Optional.of(todo)
            } else {
                return@execute Optional.empty()
            }
        }
    }

    /**
     * Delete the TODO entry identified by the given ID in from the database.
     * @param tracingSpan the tracing span to use
     * @param id the ID identifying the entry to delete
     * @param userId the database user id
     * @return the deleted entry as `Optional`
     */
    fun delete(tracingSpan: SpanContext,
               id: String,
               userId: String): Optional<Todo> {
        return execute(tracingSpan, "cassandra::delete"
        ) {
            getNoContext(id, userId)
                    .map { todo: Todo ->
                        val bs = deleteStatement.bind(id)
                        val rs = session.execute(bs)
                        if (!rs.wasApplied()) {
                            throw RuntimeException("Failed to delete todo: "
                                    + todo)
                        }
                        todo
                    }
        }
    }

    /**
     * Insert a new TODO entry in the database.
     * @param tracingSpan the tracing span to use
     * @param todo the entry to insert
     */
    fun insert(tracingSpan: SpanContext, todo: Todo) {
        execute<Any?>(tracingSpan, "cassandra::insert") {
            val bs = insertStatement
                    .bind(todo.id,
                            todo.userId,
                            todo.title,
                            todo.completed,
                            Date(todo.created))
            val execute = session.execute(bs)
            if (!execute.wasApplied()) {
                throw RuntimeException("Failed to insert todo: "
                        + todo)
            }
            null
        }
    }

    companion object {
        /**
         * The database query for retrieving all entries.
         */
        private const val LIST_QUERY = "select * from backend where user = ? ALLOW FILTERING"

        /**
         * The database query for retrieving a single entry.
         */
        private const val GET_QUERY = "select * from backend where id = ?"

        /**
         * The database query for inserting a new entry.
         */
        private const val INSERT_QUERY = ("insert into backend (id, user, message, completed, created)"
                + " values (?, ?, ?, ?, ?)")

        /**
         * The database query for updating an existing entry.
         */
        private const val UPDATE_QUERY = ("update backend set message = ?, completed = ?"
                + " where id = ? if user = ?")

        /**
         * The database query for deleting an entry.
         */
        private const val DELETE_QUERY = "delete from backend where id = ?"

        /**
         * Invoke the given supplier and wrap it around with a tracing
         * `Span`.
         * @param <T> the supplier return type
         * @param tracingSpan the parent span to use
         * @param operation the name of the operation
         * @param supplier the supplier to invoke
         * @return the object returned by the supplier
        </T> */
        private fun <T> execute(tracingSpan: SpanContext,
                                operation: String,
                                supplier: Supplier<T>): T {
            val span = startSpan(tracingSpan, operation)
            return try {
                supplier.get()
            } catch (e: Exception) {
                Tags.ERROR[span] = true
                span.log(Map.of("event", "error",
                        "error.object", e))
                throw e
            } finally {
                span.finish()
            }
        }

        /**
         * Utility method to create and start a child span of the given span.
         * @param span the parent span
         * @param operation the name for the new span
         * @return the created span
         */
        private fun startSpan(span: SpanContext,
                              operation: String): Span {
            return GlobalTracer.get()
                    .buildSpan(operation).asChildOf(span).start()
        }
    }

    /**
     * Create a new `DbService` instance.
     */
    init {
        val clusterBuilder = Cluster.builder()
        val cConfig = config["cassandra"]
        cConfig["servers"].asList(Config::class.java).get().forEach(Consumer { serverConfig: Config ->
            clusterBuilder.addContactPoints(
                    serverConfig["host"].asString().get())
        })
        cConfig["port"].asInt().ifPresent { port: Int? -> clusterBuilder.withPort(port!!) }
        cluster = clusterBuilder.build()
        session = cluster.connect("backend")
        listStatement = session.prepare(LIST_QUERY)
        getStatement = session.prepare(GET_QUERY)
        insertStatement = session.prepare(INSERT_QUERY)
        updateStatement = session.prepare(UPDATE_QUERY)
        deleteStatement = session.prepare(DELETE_QUERY)
    }
}