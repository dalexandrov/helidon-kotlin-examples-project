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

import io.helidon.config.Config
import java.util.*
import java.util.concurrent.CompletionStage

/**
 * Interface for Data Access Objects.
 *
 *
 * As Helidon SE is a reactive framework, we cannot block it.
 * Method on this interface return a [java.util.concurrent.CompletionStage] with the data, so it
 * can be correctly handled by the server.
 *
 *
 * Methods in implementation must not block thread
 */
interface EmployeeRepository {
    /**
     * Returns the list of the employees.
     * @return The collection of all the employee objects
     */
    val all: CompletionStage<List<Employee?>?>

    /**
     * Returns the list of the employees that match with the specified lastName.
     * @param lastName Represents the last name value for the search.
     * @return The collection of the employee objects that match with the specified
     * lastName
     */
    fun getByLastName(lastName: String?): CompletionStage<List<Employee?>?>

    /**
     * Returns the list of the employees that match with the specified title.
     * @param title Represents the title value for the search
     * @return The collection of the employee objects that match with the specified
     * title
     */
    fun getByTitle(title: String?): CompletionStage<List<Employee?>?>

    /**
     * Returns the list of the employees that match with the specified department.
     * @param department Represents the department value for the search.
     * @return The collection of the employee objects that match with the specified
     * department
     */
    fun getByDepartment(department: String?): CompletionStage<List<Employee?>?>

    /**
     * Add a new employee.
     * @param employee returns the employee object including the ID generated.
     * @return the employee object including the ID generated
     */
    fun save(employee: Employee?): CompletionStage<Employee?> // Add new employee

    /**
     * Update an existing employee.
     * @param updatedEmployee The employee object with the values to update
     * @param id The employee ID
     * @return number of updated records
     */
    fun update(updatedEmployee: Employee?, id: String?): CompletionStage<Long?>

    /**
     * Delete an employee by ID.
     * @param id The employee ID
     * @return number of deleted records
     */
    fun deleteById(id: String?): CompletionStage<Long?>

    /**
     * Get an employee by ID.
     * @param id The employee ID
     * @return The employee object if the employee is found
     */
    fun getById(id: String?): CompletionStage<Optional<Employee?>?>

    companion object {
        /**
         * Create a new employeeRepository instance using one of the two implementations
         * [EmployeeRepositoryImpl] or [EmployeeRepositoryImplDB] depending
         * on the specified driver type.
         * @param driverType Represents the driver type. It can be Array or Oracle.
         * @param config Contains the application configuration specified in the
         * `application.yaml` file.
         * @return The employee repository implementation.
         */
        @JvmStatic
        fun create(driverType: String, config: Config): EmployeeRepository {
            return when (driverType) {
                "Database" -> EmployeeRepositoryImplDB(config)
                "Array" ->             // Array is default
                    EmployeeRepositoryImpl()
                else -> EmployeeRepositoryImpl()
            }
        }
    }
}