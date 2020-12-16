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
package io.helidon.kotlin.examples.dbclient.common

import asType
import io.helidon.dbclient.DbMapper
import io.helidon.dbclient.DbRow
import java.util.*

/**
 * Maps database statements to [Pokemon] class.
 */
class PokemonMapper : DbMapper<Pokemon> {
    override fun read(row: DbRow): Pokemon {
        var name = row.column("name")
        // we know that in mongo this is not true
        if (null == name) {
            name = row.column("_id")
        }
        val type = row.column("type")
        return Pokemon(name!!.asType(String::class.java), type.asType(String::class.java))
    }

    override fun toNamedParameters(value: Pokemon): Map<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap(1)
        map["name"] = value.name
        map["type"] = value.type
        return map
    }

    override fun toIndexedParameters(value: Pokemon): List<Any?> {
        val list: MutableList<Any?> = ArrayList(2)
        list.add(value.name)
        list.add(value.type)
        return list
    }
}