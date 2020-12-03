/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.integrations.examples.datasource.hikaricp.jaxrs

import java.sql.SQLException
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.inject.Named
import javax.sql.DataSource
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A JAX-RS resource class in [ application scope][ApplicationScoped] rooted at `/tables`.
 *
 * @see .get
 */
@Path("/tables")
@ApplicationScoped
open class TablesResource @Inject constructor(@Named("example") dataSource: DataSource) {
    private val dataSource: DataSource = Objects.requireNonNull(dataSource)

    /**
     * Returns a [Response] which, if successful, contains a
     * newline-separated list of Oracle database table names.
     *
     *
     * This method never returns `null`.
     *
     * @return a non-`null` [Response]
     *
     * @exception SQLException if a database error occurs
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Throws(SQLException::class)
    open fun get(): Response {
        val sb = StringBuilder()
        dataSource.connection.use { connection ->
            connection.prepareStatement(" SELECT TABLE_NAME"
                    + " FROM ALL_TABLES "
                    + "ORDER BY TABLE_NAME ASC").use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        sb.append(rs.getString(1)).append("\n")
                    }
                }
            }
        }
        return Response.ok()
                .entity(sb.toString())
                .build()
    }

}