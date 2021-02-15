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
package io.helidon.kotlin.security.examples.security

import io.helidon.security.Security
import io.helidon.security.SecurityContext
import io.helidon.security.SecurityResponse
import io.helidon.security.Subject
import security
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ExecutionException

/**
 * This class shows how to manually secure your application.
 */
class ProgrammaticSecurity {
    private lateinit var security: Security
    private fun multithreaded(subject: Subject) {
        val thread = Thread {
            try {
                val context = security.contextBuilder("newThread")
                        .build()
                CONTEXT.set(context)

                //this must be done, as there is no subject (yet) for current thread (or event the login attempt may be done
                //in this thread - depends on what your application wants to do...
                context.runAs(subject) {

                    //3: authorize access to restricted resource
                    execute()
                    //4: propagate identity
                    propagate()
                }
            } finally {
                CONTEXT.remove()
            }
        }
        thread.start()
    }

    private fun propagate() {
        val response = CONTEXT.get().outboundClientBuilder().buildAndGet()
        when (response.status()) {
            SecurityResponse.SecurityStatus.SUCCESS ->             //we should have "Authorization" header present and just need to update request headers of our outbound call
                println("Authorization header: " + response.requestHeaders()["Authorization"])
            SecurityResponse.SecurityStatus.SUCCESS_FINISH -> println("Identity propagation done, request sent...")
            else -> println("Failed in identity propagation provider: " + response.description().orElse(null))
        }
    }

    private fun execute() {
        val context = CONTEXT.get()
        //check role
        check(context.isUserInRole("theRole")) { "User is not in expected role" }
        context.env(context.env()
                .derive()
                .addAttribute("resourceType", "CustomResourceType"))

        //check authorization through provider
        val response = context.atzClientBuilder().buildAndGet()
        if (response.status().isSuccess) {
            //ok, process resource
            println("Resource processed")
        } else {
            println("You are not permitted to process resource")
        }
    }

    private fun login(): Subject {
        val securityContext = CONTEXT.get()
        securityContext.env(securityContext.env().derive()
                .path("/some/path")
                .header("Authorization", buildBasic("aUser", "aPassword")))
        val response = securityContext.atnClientBuilder().buildAndGet()
        if (response.status().isSuccess) {
            return response.user().orElseThrow { IllegalStateException("No user authenticated!") }
        }
        throw RuntimeException("Failed to authenticate", response.throwable().orElse(null))
    }

    private fun init() {
        //binds security context to current thread
        security = security {
            addProvider(MyProvider(), "FirstProvider")
        }
        CONTEXT.set(security.contextBuilder("mainThread").build())
    }

    companion object {
        private val CONTEXT = ThreadLocal<SecurityContext>()

        /**
         * Entry point to this example.
         *
         * @param args no needed
         * @throws ExecutionException   if asynchronous security fails
         * @throws InterruptedException if asynchronous security gets interrupted
         */
        @Throws(ExecutionException::class, InterruptedException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val instance = ProgrammaticSecurity()

            /*
         * Simple single threaded applications - nothing too complicated
         */
            //1: initialize security component
            instance.init()
            //2: login
            val subject = instance.login()

            //3: authorize access to restricted resource
            instance.execute()
            //4: propagate identity
            instance.propagate()

            /*
         * More complex - multithreaded application
         */instance.multithreaded(subject)
        }

        private fun buildBasic(user: String, password: String): String {
            return "basic " + Base64.getEncoder()
                    .encodeToString("$user:$password".toByteArray(StandardCharsets.UTF_8))
        }
    }
}