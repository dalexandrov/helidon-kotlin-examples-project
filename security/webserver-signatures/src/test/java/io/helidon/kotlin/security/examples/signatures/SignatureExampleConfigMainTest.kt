/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.signatures

import io.helidon.kotlin.security.examples.signatures.SignatureExampleConfigMain.main
import io.helidon.kotlin.security.examples.signatures.SignatureExampleConfigMain.service1Server
import io.helidon.kotlin.security.examples.signatures.SignatureExampleConfigMain.service2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

/**
 * Unit test for [SignatureExampleBuilderMain].
 */
class SignatureExampleConfigMainTest : SignatureExampleTest() {


    companion object {
        private var svc1Port = 0
        private var svc2Port = 0

        @BeforeAll
        @JvmStatic
        fun initClass() {
            main(emptyArray())
            svc1Port = service1Server.port()
            svc2Port = service2Server.port()
        }

        @AfterAll
        @JvmStatic
        @Throws(InterruptedException::class)
        fun destroyClass() {
            stopServer(service2Server)
            stopServer(service1Server)
        }
    }

    override val service1Port: Int
        get() = service1Port
    override val service2Port: Int
        get() = service2Port
}