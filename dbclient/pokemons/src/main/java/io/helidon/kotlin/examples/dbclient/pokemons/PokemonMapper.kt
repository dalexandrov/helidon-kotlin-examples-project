/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.examples.dbclient.pokemons

import asType
import io.helidon.dbclient.DbMapper
import io.helidon.dbclient.DbRow
import java.util.*

/**
 * Maps database statements to [io.helidon.kotlin.examples.dbclient.common.Pokemon] class.
 */
class PokemonMapper : DbMapper<Pokemon> {
    override fun read(row: DbRow): Pokemon {
        val id = row.column("id")
        val name = row.column("name")
        val type = row.column("idType")
        return Pokemon(id.asType(Int::class.java), name.asType(String::class.java), type.asType(Int::class.java))
    }

    override fun toNamedParameters(value: Pokemon): Map<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap(3)
        map["id"] = value.id
        map["name"] = value.name
        map["idType"] = value.idType
        return map
    }

    override fun toIndexedParameters(value: Pokemon): List<Any?> {
        val list: MutableList<Any?> = ArrayList(3)
        list.add(value.id)
        list.add(value.name)
        list.add(value.idType)
        return list
    }
}