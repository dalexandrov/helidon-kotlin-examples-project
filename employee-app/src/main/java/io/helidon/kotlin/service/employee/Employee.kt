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
package io.helidon.kotlin.service.employee

import java.util.*
import javax.json.bind.annotation.JsonbCreator
import javax.json.bind.annotation.JsonbProperty

/**
 * Represents an employee.
 */
class Employee
/**Creates a new Employee.
 * @param id The employee ID.
 * @param firstName The employee first name.
 * @param lastName The employee lastName.
 * @param email The employee email.
 * @param phone The employee phone.
 * @param birthDate The employee birthDatee.
 * @param title The employee title.
 * @param department The employee department.
 */ private constructor(
        /**
         * Returns the employee ID.
         * @return the ID
         */
        val id: String,
        /**
         * Returns the employee first name.
         * @return The first name
         */
        val firstName: String,
        /**
         * Returns the employee last name.
         * @return The last name
         */
        val lastName: String,
        /**
         * Returns the employee e-mail.
         * @return The email
         */
        val email: String,
        /**
         * Returns the employee phone.
         * @return The phone
         */
        val phone: String,
        /**
         * Returns the employee birthdate.
         * @return The birthdate
         */
        val birthDate: String,
        /**
         * Returns the employee title.
         * @return The title
         */
        val title: String,
        /**
         * Returns the employee department.
         * @return The department
         */
        val department: String) {
    override fun toString(): String {
        return ("ID: " + id + " First Name: " + firstName + " Last Name: " + lastName + " EMail: " + email + " Phone: "
                + phone + " Birth Date: " + birthDate + " Title: " + title + " Department: " + department)
    }

    companion object {
        /**
         * Creates a new employee. This method helps to parse the json parameters in the requests.
         * @param id The employee ID. If the employee ID is null or empty generates a new ID.
         * @param firstName The employee first name.
         * @param lastName The employee lastName.
         * @param email The employee email.
         * @param phone The employee phone.
         * @param birthDate The employee birthDatee.
         * @param title The employee title.
         * @param department The employee department.
         * @return A new employee object
         */
        @JvmStatic
        @JsonbCreator
        fun of(@JsonbProperty("id") id: String?, @JsonbProperty("firstName") firstName: String,
               @JsonbProperty("lastName") lastName: String, @JsonbProperty("email") email: String,
               @JsonbProperty("phone") phone: String, @JsonbProperty("birthDate") birthDate: String,
               @JsonbProperty("title") title: String, @JsonbProperty("department") department: String): Employee {
            var id = id
            if (id == null || id.trim { it <= ' ' } == "") {
                id = UUID.randomUUID().toString()
            }
            return Employee(id, firstName, lastName, email, phone, birthDate, title, department)
        }
    }
}