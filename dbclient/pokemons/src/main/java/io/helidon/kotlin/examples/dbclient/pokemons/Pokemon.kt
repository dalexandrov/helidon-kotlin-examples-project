/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates. All rights reserved.
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

import io.helidon.common.Reflected

/**
 * POJO representing Pokemon.
 */
@Reflected
class Pokemon {
    var id = 0
    var name: String? = null
    var idType = 0

    /**
     * Default constructor.
     */
    constructor() {
        // JSON-B
    }

    /**
     * Create pokemon with name and type.
     *
     * @param id id of the beast
     * @param name name of the beast
     * @param idType id of beast type
     */
    constructor(id: Int, name: String?, idType: Int) {
        this.name = name
        this.idType = idType
    }
}