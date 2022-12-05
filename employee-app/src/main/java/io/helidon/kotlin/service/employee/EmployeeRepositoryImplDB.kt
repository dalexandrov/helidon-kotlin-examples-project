/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.service.employee

import io.helidon.common.reactive.Multi
import io.helidon.config.Config
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbExecute
import io.helidon.dbclient.DbRow
import io.helidon.dbclient.jdbc.JdbcDbClientProviderBuilder
import io.helidon.kotlin.service.employee.Employee.Companion.of
import to
import java.util.*
import java.util.concurrent.CompletionStage

/**
 * Implementation of the [EmployeeRepository]. This implementation uses an
 * Oracle database to persist the Employee objects.
 *
 */
internal class EmployeeRepositoryImplDB(config: Config) : EmployeeRepository {
    private val dbClient: DbClient
    override val all: CompletionStage<List<Employee?>?>
        get() {
            val queryStr = "SELECT * FROM EMPLOYEE"
            return toEmployeeList(dbClient.execute { exec: DbExecute -> exec.query(queryStr) })
        }

    override fun getByLastName(name: String?): CompletionStage<List<Employee?>?> {
        val queryStr = "SELECT * FROM EMPLOYEE WHERE LASTNAME LIKE ?"
        return toEmployeeList(dbClient.execute { exec: DbExecute -> exec.query(queryStr, name) })
    }

    override fun getByTitle(title: String?): CompletionStage<List<Employee?>?> {
        val queryStr = "SELECT * FROM EMPLOYEE WHERE TITLE LIKE ?"
        return toEmployeeList(dbClient.execute { exec: DbExecute -> exec.query(queryStr, title) })
    }

    override fun getByDepartment(department: String?): CompletionStage<List<Employee?>?> {
        val queryStr = "SELECT * FROM EMPLOYEE WHERE DEPARTMENT LIKE ?"
        return toEmployeeList(dbClient.execute { exec: DbExecute -> exec.query(queryStr, department) })
    }

    override fun save(employee: Employee?): CompletionStage<Employee?> {
        val insertTableSQL = ("INSERT INTO EMPLOYEE "
                + "(ID, FIRSTNAME, LASTNAME, EMAIL, PHONE, BIRTHDATE, TITLE, DEPARTMENT) "
                + "VALUES(EMPLOYEE_SEQ.NEXTVAL,?,?,?,?,?,?,?)")
        return dbClient.execute { exec: DbExecute ->
            exec.createInsert(insertTableSQL)
                    .addParam(employee!!.firstName)
                    .addParam(employee.lastName)
                    .addParam(employee.email)
                    .addParam(employee.phone)
                    .addParam(employee.birthDate)
                    .addParam(employee.title)
                    .addParam(employee.department)
                    .execute()
        } // let's always return the employee once the insert finishes
                .thenApply { employee }
    }

    override fun deleteById(id: String?): CompletionStage<Long?> {
        val deleteRowSQL = "DELETE FROM EMPLOYEE WHERE ID=?"
        return dbClient.execute { exec: DbExecute -> exec.delete(deleteRowSQL, id) }
    }

    override fun getById(id: String?): CompletionStage<Optional<Employee?>?> {
        val queryStr = "SELECT * FROM EMPLOYEE WHERE ID =?"
        return dbClient.execute { exec: DbExecute -> exec[queryStr, id] }
                .map { optionalRow: Optional<DbRow> -> optionalRow.map { dbRow: DbRow -> dbRow.to<Employee>() } }
    }

    override fun update(updatedEmployee: Employee?, id: String?): CompletionStage<Long?> {
        val updateTableSQL = ("UPDATE EMPLOYEE SET FIRSTNAME=?, LASTNAME=?, EMAIL=?, PHONE=?, BIRTHDATE=?, TITLE=?, "
                + "DEPARTMENT=?  WHERE ID=?")
        return dbClient.execute { exec: DbExecute ->
            exec.createUpdate(updateTableSQL)
                    .addParam(updatedEmployee!!.firstName)
                    .addParam(updatedEmployee.lastName)
                    .addParam(updatedEmployee.email)
                    .addParam(updatedEmployee.phone)
                    .addParam(updatedEmployee.birthDate)
                    .addParam(updatedEmployee.title)
                    .addParam(updatedEmployee.department)
                    .addParam(id!!.toInt())
                    .execute()
        }
    }

    private object EmployeeDbMapper {
        fun read(row: DbRow): Employee {
            // map named columns to an object
            return of(
                    row.column("ID").to<String>(),
                    row.column("FIRSTNAME").to<String>(),
                    row.column("LASTNAME").to<String>(),
                    row.column("EMAIL").to<String>(),
                    row.column("PHONE").to<String>(),
                    row.column("BIRTHDATE").to<String>(),
                    row.column("TITLE").to<String>(),
                    row.column("DEPARTMENT").to<String>()
            )
        }
    }

    companion object {
        private fun toEmployeeList(resultSet: Multi<DbRow>): CompletionStage<List<Employee?>?> {
            return resultSet.map { obj: DbRow? -> obj?.let { EmployeeDbMapper.read(it) } }
                    .collectList()
        }
    }

    /**
     * Creates the database connection using the parameters specified in the
     * `application.yaml` file located in the `resources` directory.
     * @param config Represents the application configuration.
     */
    init {
        val url = "jdbc:oracle:thin:@"
        val driver = "oracle.jdbc.driver.OracleDriver"
        val dbUserName = config["app.user"].asString().orElse("sys as SYSDBA")
        val dbUserPassword = config["app.password"].asString().orElse("password")
        val dbHostURL = config["app.hosturl"].asString().orElse("localhost:1521/xe")
        try {
            Class.forName(driver)
        } catch (sqle: Exception) {
            sqle.printStackTrace()
        }

        // now we create the reactive DB Client - explicitly use JDBC, so we can
        // configure JDBC specific configuration
        dbClient = JdbcDbClientProviderBuilder.create()
                .url(url + dbHostURL)
                .username(dbUserName)
                .password(dbUserPassword)
                .build()
    }
}