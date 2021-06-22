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
/*
 This class is almost exactly copied from Micronaut examples.
 */
package io.helidon.examples.integrations.micronaut.data

import io.helidon.examples.integrations.micronaut.data.model.Owner
import io.helidon.examples.integrations.micronaut.data.model.Pet
import io.micronaut.context.event.StartupEvent
import io.micronaut.core.annotation.TypeHint
import io.micronaut.runtime.event.annotation.EventListener
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A Micronaut bean that listens on startup event and populates database with data.
 */
@Singleton
@TypeHint(typeNames = ["org.h2.Driver", "org.h2.mvstore.db.MVTableEngine"])
open class DbPopulateData @Inject internal constructor(
    private val ownerRepository: DbOwnerRepository,
    private val petRepository: DbPetRepository
) {
    @EventListener
    open fun init(event: StartupEvent?) {
        val fred = Owner("Fred")
        fred.age = 45
        val barney = Owner("Barney")
        barney.age = 40
        ownerRepository.saveAll(Arrays.asList(fred, barney))
        val dino = Pet("Dino", fred)
        val bp = Pet("Baby Puss", fred)
        bp.type = Pet.PetType.CAT
        val hoppy = Pet("Hoppy", barney)
        petRepository.saveAll(Arrays.asList(dino, bp, hoppy))
    }
}