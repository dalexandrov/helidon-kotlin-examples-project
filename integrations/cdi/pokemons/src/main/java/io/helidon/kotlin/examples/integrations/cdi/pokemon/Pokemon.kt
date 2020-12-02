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

import javax.json.bind.annotation.JsonbTransient
import javax.persistence.*

/**
 * A Pokemon entity class. A Pokemon is represented as a triple of an
 * ID, a name and a type.
 */
@Entity(name = "Pokemon")
@Table(name = "POKEMON")
@Access(AccessType.PROPERTY)
@NamedQueries(NamedQuery(name = "getPokemons", query = "SELECT p FROM Pokemon p"), NamedQuery(name = "getPokemonByName", query = "SELECT p FROM Pokemon p WHERE p.name = :name"))
open class Pokemon
/**
 * Creates a new pokemon.
 */
{
    @get:Column(name = "ID", nullable = false, updatable = false)
    @get:Id
    var id = 0

    @get:Column(name = "NAME", nullable = false)
    @get:Basic(optional = false)
    open var name: String? = null

    /**
     * Returns pokemon's type.
     *
     * @return Pokemon's type.
     */
    @get:ManyToOne
    @JsonbTransient
    var pokemonType: PokemonType? = null
        private set

    @get:Transient
    open var type = 0

    /**
     * Sets pokemon's type.
     *
     * @param pokemonType Pokemon's type.
     */
    open fun setPokemonType(pokemonType: PokemonType) {
        this.pokemonType = pokemonType
        type = pokemonType.id
    }
}