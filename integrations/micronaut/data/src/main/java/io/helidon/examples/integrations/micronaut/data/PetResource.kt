/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.examples.integrations.micronaut.data

import io.helidon.examples.integrations.micronaut.data.model.Pet
import javax.ws.rs.GET
import org.eclipse.microprofile.metrics.annotation.SimplyTimed
import javax.inject.Inject
import javax.validation.constraints.Pattern
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.PathParam

/**
 * JAX-RS resource, and the MicroProfile entry point to manage pets.
 * This resource used Micronaut data beans (repositories) to query database, and
 * bean validation as implemented by Micronaut.
 */
@Path("/pets")
open class PetResource
/**
 * Create a new instance with pet repository.
 *
 * @param petRepo Pet repository from Micronaut data
 */ @Inject constructor(private var petRepository: DbPetRepository) {
    /**
     * Gets all pets from the database.
     * @return all pets, using JSON-B to map them to JSON
     */
    @get:GET
    open val all: Iterable<Pet?>
        get() = petRepository.findAll()

    /**
     * Get a named pet from the database.
     *
     * @param name name of the pet to find, must be at least two characters long, may contain whitespace
     * @return a single pet
     * @throws javax.ws.rs.NotFoundException in case the pet is not in the database (to return 404 status)
     */
    @Path("/{name}")
    @GET
    @SimplyTimed
    open fun pet(@PathParam("name") name: @Pattern(regexp = "\\w+[\\w+\\s?]*\\w") String?): Pet {
        return petRepository.findByName(name)
            ?.orElseThrow { NotFoundException("Pet by name $name does not exist") }!!
    }
}