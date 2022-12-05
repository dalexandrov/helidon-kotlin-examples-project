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
package io.helidon.kotlin.examples.dbclient.pokemons

import io.helidon.common.reactive.Single
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbExecute
import javax.json.Json

/**
 * Initialize JDBC database schema and populate it with sample data.
 */
class InitializeDb private constructor() {
    companion object {
        /** Pokemon types source file.  */
        private const val TYPES = "/Types.json"

        /** Pokemons source file.  */
        private const val POKEMONS = "/Pokemons.json"

        /**
         * Initialize JDBC database schema and populate it with sample data.
         * @param dbClient database client
         */
        @JvmStatic
        fun init(dbClient: DbClient) {
            try {
                if (!isMongo) {
                    initSchema(dbClient)
                }
                initData(dbClient)
            } catch (ex: Exception) {
                System.out.printf("Could not initialize database: %s\n", ex.message)
            }
        }

        /**
         * Initializes database schema (tables).
         *
         * @param dbClient database client
         */
        private fun initSchema(dbClient: DbClient) {
            try {
                dbClient.execute { exec: DbExecute ->
                    exec
                        .namedDml("create-types")
                        .flatMapSingle { exec.namedDml("create-pokemons") }
                }
                    .await()
            } catch (ex1: Exception) {
                System.out.printf("Could not create tables: %s", ex1.message)
                try {
                    deleteData(dbClient)
                } catch (ex2: Exception) {
                    System.out.printf("Could not delete tables: %s", ex2.message)
                }
            }
        }

        /**
         * InitializeDb database content (rows in tables).
         *
         * @param dbClient database client
         */
        private fun initData(dbClient: DbClient) {
            // Init pokemon types
            dbClient.execute { exec: DbExecute ->
                initTypes(exec)
                    .flatMapSingle { initPokemons(exec) }
            }
                .await()
        }

        /**
         * Delete content of all tables.
         *
         * @param dbClient database client
         */
        private fun deleteData(dbClient: DbClient) {
            dbClient.execute { exec: DbExecute ->
                exec
                    .namedDelete("delete-all-pokemons")
                    .flatMapSingle { exec.namedDelete("delete-all-types") }
            }
                .await()
        }

        /**
         * Initialize pokemon types.
         * Source data file is JSON file containing array of type objects:
         * <pre>
         * [
         * { "id": <type_id>, "name": <type_name> },
         * ...
         * ]
        </type_name></type_id></pre> *
         * where `id` is JSON number and {@ocde name} is JSON String.
         *
         * @param exec database client executor
         * @return executed statements future
         */
        private fun initTypes(exec: DbExecute): Single<Long> {
            var stage = Single.just(0L)
            Json.createReader(InitializeDb::class.java.getResourceAsStream(TYPES)).use { reader ->
                val types = reader.readArray()
                for (typeValue in types) {
                    val type = typeValue.asJsonObject()
                    stage = stage.flatMapSingle {
                        exec.namedInsert(
                            "insert-type", type.getInt("id"), type.getString("name")
                        )
                    }
                }
            }
            return stage
        }

        /**
         * Initialize pokemos.
         * Source data file is JSON file containing array of type objects:
         * <pre>
         * [
         * { "id": <type_id>, "name": <type_name>, "type": [<type_id>, <type_id>, ...] },
         * ...
         * ]
        </type_id></type_id></type_name></type_id></pre> *
         * where `id` is JSON number and {@ocde name} is JSON String.
         *
         * @param exec database client executor
         * @return executed statements future
         */
        private fun initPokemons(exec: DbExecute): Single<Long?> {
            var stage = Single.just(0L)
            Json.createReader(InitializeDb::class.java.getResourceAsStream(POKEMONS)).use { reader ->
                val pokemons = reader.readArray()
                for (pokemonValue in pokemons) {
                    val pokemon = pokemonValue.asJsonObject()
                    stage = stage.flatMapSingle {
                        exec
                            .namedInsert(
                                "insert-pokemon",
                                pokemon.getInt("id"), pokemon.getString("name"), pokemon.getInt("idType")
                            )
                    }
                }
            }
            return stage
        }
    }

    /**
     * Creates an instance of database initialization.
     */
    init {
        throw UnsupportedOperationException("Instances of InitializeDb utility class are not allowed")
    }
}