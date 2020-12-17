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
package io.helidon.kotlin.examples.integrations.cdi.pokemon

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.transaction.Transactional
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 * This class implements REST endpoints to interact with Pokemons. The following
 * operations are supported:
 *
 * GET /pokemon: Retrieve list of all pokemons
 * GET /pokemon/{id}: Retrieve single pokemon by ID
 * GET /pokemon/name/{name}: Retrieve single pokemon by name
 * DELETE /pokemon/{id}: Delete a pokemon by ID
 * POST /pokemon: Create a new pokemon
 */
@Path("pokemon")
open class PokemonResource {
    @PersistenceContext(unitName = "test")
    lateinit var entityManager: EntityManager

    /**
     * Retrieves list of all pokemons.
     *
     * @return List of pokemons.
     */
    @get:Produces(MediaType.APPLICATION_JSON)
    @get:GET
    open val pokemons
        get() = entityManager.createNamedQuery("getPokemons", Pokemon::class.java).resultList

    /**
     * Retrieves single pokemon by ID.
     *
     * @param id The ID.
     * @return A pokemon that matches the ID.
     * @throws NotFoundException If no pokemon found for the ID.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    open fun getPokemonById(@PathParam("id") id: String): Pokemon {
        return try {
            entityManager.find(Pokemon::class.java, Integer.valueOf(id))
        } catch (e: IllegalArgumentException) {
            throw NotFoundException("Unable to find pokemon with ID $id")
        }
    }

    /**
     * Deletes a single pokemon by ID.
     *
     * @param id The ID.
     */
    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional(Transactional.TxType.REQUIRED)
    open fun deletePokemon(@PathParam("id") id: String) {
        val pokemon = getPokemonById(id)
        entityManager.remove(pokemon)
    }

    /**
     * Retrieves a pokemon by name.
     *
     * @param name The name.
     * @return A pokemon that matches the name.
     * @throws NotFoundException If no pokemon found for the name.
     */
    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    open fun getPokemonByName(@PathParam("name") name: String): Pokemon {
        val query = entityManager.createNamedQuery("getPokemonByName", Pokemon::class.java)
        val list = query.setParameter("name", name).resultList
        if (list.isEmpty()) {
            throw NotFoundException("Unable to find pokemon with name $name")
        }
        return list[0]
    }

    /**
     * Creates a new pokemon.
     *
     * @param pokemon New pokemon.
     * @throws BadRequestException If a problem was found.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(Transactional.TxType.REQUIRED)
    open fun createPokemon(pokemon: Pokemon) {
        try {
            val pokemonType = entityManager.createNamedQuery("getPokemonTypeById", PokemonType::class.java)
                    .setParameter("id", pokemon.type).singleResult
            pokemon.setPokemonType(pokemonType)
            entityManager.persist(pokemon)
        } catch (e: Exception) {
            throw BadRequestException("Unable to create pokemon with ID " + pokemon.id)
        }
    }
}