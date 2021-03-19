/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

import io.helidon.config.Config
import io.helidon.kotlin.service.employee.Employee.Companion.of
import io.helidon.kotlin.service.employee.EmployeeRepository.Companion.create
import io.helidon.webserver.*
import single
import java.util.*
import java.util.logging.Logger

/**
 * The Employee service endpoints. Get all employees: curl -X GET
 * http://localhost:8080/employees Get employee by id: curl -X GET
 * http://localhost:8080/employees/{id} Add employee curl -X POST
 * http://localhost:8080/employees/{id} Update employee by id curl -X PUT
 * http://localhost:8080/employees/{id} Delete employee by id curl -X DELETE
 * http://localhost:8080/employees/{id} The message is returned as a JSON object
 */
class EmployeeService internal constructor(config: Config) : Service {
    private var employees: EmployeeRepository = create(config["app.drivertype"]
            .asString()
            .orElse("Array"),
            config)

    /**
     * A service registers itself by updating the routine rules.
     * @param rules the routing rules.
     */
    override fun update(rules: Routing.Rules) {
        rules["/", Handler { _: ServerRequest, response: ServerResponse -> getAll(response) }]["/lastname/{name}", Handler { request: ServerRequest, response: ServerResponse -> getByLastName(request, response) }]["/department/{name}", Handler { request: ServerRequest, response: ServerResponse -> getByDepartment(request, response) }]["/title/{name}", Handler { request: ServerRequest, response: ServerResponse -> getByTitle(request, response) }]
                .post("/", Handler { request: ServerRequest, response: ServerResponse -> save(request, response) })["/{id}", Handler { request: ServerRequest, response: ServerResponse -> getEmployeeById(request, response) }]
                .put("/{id}", Handler { request: ServerRequest, response: ServerResponse -> this.update(request, response) })
                .delete("/{id}", Handler { request: ServerRequest, response: ServerResponse -> delete(request, response) })
    }

    /**
     * Gets all the employees.
     * @param response the server response
     */
    private fun getAll(response: ServerResponse) {
        LOGGER.fine("getAll")
        employees
                .all
                .thenAccept { t: List<Employee?>? -> response.send(t) }
                .exceptionally { throwable: Throwable? -> response.send(throwable) }
    }

    /**
     * Gets the employees by the last name specified in the parameter.
     * @param request  the server request
     * @param response the server response
     */
    private fun getByLastName(request: ServerRequest, response: ServerResponse) {
        LOGGER.fine("getByLastName")
        val name = request.path().param("name")
        // Invalid query strings handled in isValidQueryStr. Keeping DRY
        if (isValidQueryStr(response, name)) {
            employees.getByLastName(name)
                    .thenAccept { t: List<Employee?>? -> response.send(t) }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        }
    }

    /**
     * Gets the employees by the title specified in the parameter.
     * @param request  the server request
     * @param response the server response
     */
    private fun getByTitle(request: ServerRequest, response: ServerResponse) {
        LOGGER.fine("getByTitle")
        val title = request.path().param("name")
        if (isValidQueryStr(response, title)) {
            employees.getByTitle(title)
                    .thenAccept { t: List<Employee?>? -> response.send(t) }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        }
    }

    /**
     * Gets the employees by the department specified in the parameter.
     * @param request  the server request
     * @param response the server response
     */
    private fun getByDepartment(request: ServerRequest, response: ServerResponse) {
        LOGGER.fine("getByDepartment")
        val department = request.path().param("name")
        if (isValidQueryStr(response, department)) {
            employees.getByDepartment(department)
                    .thenAccept { t: List<Employee?>? -> response.send(t) }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        }
    }

    /**
     * Gets the employees by the ID specified in the parameter.
     * @param request  the server request
     * @param response the server response
     */
    private fun getEmployeeById(request: ServerRequest, response: ServerResponse) {
        LOGGER.fine("getEmployeeById")
        val id = request.path().param("id")
        // If invalid, response handled in isValidId. Keeping DRY
        if (isValidQueryStr(response, id)) {
            employees.getById(id)
                    .thenAccept {
                        if (it!!.isPresent) {
                            // found
                            response.send(it.get())
                        } else {
                            // not found
                            response.status(404).send()
                        }
                    }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        }
    }

    /**
     * Saves a new employee.
     * @param request  the server request
     * @param response the server response
     */
    private fun save(request: ServerRequest, response: ServerResponse) {
        LOGGER.fine("save")
        request.content()
                .single<Employee>()
                .thenApply { e: Employee ->
                    of(null,
                            e.firstName,
                            e.lastName,
                            e.email,
                            e.phone,
                            e.birthDate,
                            e.title,
                            e.department)
                }
                .thenCompose { employee: Employee? -> employees.save(employee) }
                .thenAccept { response.status(201).send() }
                .exceptionally { throwable: Throwable? -> response.send(throwable) }
    }

    /**
     * Updates an existing employee.
     * @param request  the server request
     * @param response the server response
     */
    private fun update(request: ServerRequest, response: ServerResponse) {
        LOGGER.fine("update")
        val id = request.path().param("id")
        if (isValidQueryStr(response, id)) {
            request.content()
                    .single<Employee>()
                    .thenCompose { e: Employee? -> employees.update(e, id) }
                    .thenAccept { count: Long? ->
                        if (count == 0L) {
                            response.status(404).send()
                        } else {
                            response.status(204).send()
                        }
                    }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        }
    }

    /**
     * Deletes an existing employee.
     * @param request  the server request
     * @param response the server response
     */
    private fun delete(request: ServerRequest, response: ServerResponse) {
        LOGGER.fine("delete")
        val id = request.path().param("id")
        if (isValidQueryStr(response, id)) {
            employees.deleteById(id)
                    .thenAccept { count: Long? ->
                        if (count == 0L) {
                            response.status(404).send()
                        } else {
                            response.status(204).send()
                        }
                    }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        }
    }

    /**
     * Validates the parameter.
     * @param response the server response
     * @param nameStr
     * @return
     */
    private fun isValidQueryStr(response: ServerResponse, nameStr: String?): Boolean {
        val errorMessage: MutableMap<String, String> = HashMap()
        return if (nameStr == null || nameStr.isEmpty() || nameStr.length > 100) {
            errorMessage["errorMessage"] = "Invalid query string"
            response.status(400).send<Map<String, String>>(errorMessage)
            false
        } else {
            true
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(EmployeeService::class.java.name)
    }

}