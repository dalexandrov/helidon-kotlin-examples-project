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

import io.helidon.kotlin.service.employee.Employee.Companion.of
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors
import javax.json.bind.JsonbBuilder
import javax.json.bind.JsonbConfig

/**
 * Implementation of the [EmployeeRepository]. This implementation uses a
 * mock database written with in-memory ArrayList classes.
 * The strings id, name, and other search strings are validated before being
 * passed to the methods in this class.
 *
 */
class EmployeeRepositoryImpl : EmployeeRepository {
    private val eList = CopyOnWriteArrayList<Employee>()
    override fun getByLastName(name: String?): CompletionStage<List<Employee?>?> {
        val matchList = eList.stream().filter { e: Employee -> e.lastName.contains(name!!) }
                .collect(Collectors.toList())
        return CompletableFuture.completedFuture(matchList)
    }

    override fun getByTitle(title: String?): CompletionStage<List<Employee?>?> {
        val matchList = eList.stream().filter { e: Employee -> e.title.contains(title!!) }
                .collect(Collectors.toList())
        return CompletableFuture.completedFuture(matchList)
    }

    override fun getByDepartment(department: String?): CompletableFuture<List<Employee?>?> {
        val matchList = eList.stream().filter { e: Employee -> e.department.contains(department!!) }
                .collect(Collectors.toList())
        return CompletableFuture.completedFuture(matchList)
    }

    override val all: CompletionStage<List<Employee?>?>
        get() = CompletableFuture.completedFuture(eList)

    override fun getById(id: String?): CompletionStage<Optional<Employee?>?> {
        return CompletableFuture.completedFuture(eList.stream().filter { e: Employee -> e.id == id }.findFirst())
    }

    override fun save(employee: Employee?): CompletionStage<Employee?> {
        val nextEmployee = of(null,
                employee!!.firstName,
                employee.lastName,
                employee.email,
                employee.phone,
                employee.birthDate,
                employee.title,
                employee.department)
        eList.add(nextEmployee)
        return CompletableFuture.completedFuture(nextEmployee)
    }

    override fun update(updatedEmployee: Employee?, id: String?): CompletionStage<Long?> {
        deleteById(id)
        val e = of(id, updatedEmployee!!.firstName, updatedEmployee.lastName,
                updatedEmployee.email, updatedEmployee.phone, updatedEmployee.birthDate,
                updatedEmployee.title, updatedEmployee.department)
        eList.add(e)
        return CompletableFuture.completedFuture(1L)
    }

    override fun deleteById(id: String?): CompletionStage<Long?> {
        return CompletableFuture.completedFuture(eList.stream()
                .filter { e: Employee -> e.id == id }
                .findFirst()
                .map { o: Employee -> eList.remove(o) }
                .map { 1L }
                .orElse(0L))
    }

    /**
     * To load the initial data, parses the content of `employee.json`
     * file located in the `resources` directory to a list of Employee
     * objects.
     */
    init {
        val config = JsonbConfig().withFormatting(java.lang.Boolean.TRUE)
        val jsonb = JsonbBuilder.create(config)
        try {
            EmployeeRepositoryImpl::class.java.getResourceAsStream("/employees.json").use { jsonFile ->
                val employees = jsonb.fromJson(jsonFile, Array<Employee>::class.java)
                eList.addAll(listOf(*employees))
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }
}