/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import javax.persistence.*

/**
 * A Pokemon Type entity. A type is represented by an ID and a name.
 */
@Entity(name = "PokemonType")
@Table(name = "POKEMONTYPE")
@Access(AccessType.FIELD)
@NamedQueries(NamedQuery(name = "getPokemonTypes", query = "SELECT t FROM PokemonType t"), NamedQuery(name = "getPokemonTypeById", query = "SELECT t FROM PokemonType t WHERE t.id = :id"))
open class PokemonType
/**
 * Creates a new type.
 */
{
    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    open var id = 0

    @Basic(optional = false)
    @Column(name = "NAME")
    open var name: String? = null
}