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
package io.helidon.kotlin.microprofile.example.helloworld.implicit.cdi

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Produces
import javax.enterprise.inject.spi.InjectionPoint

/**
 * Producer for various resources required by this example.
 */
@ApplicationScoped
open class ResourceProducer {
    /**
     * Each injection will increase the COUNTER.
     *
     * @return increased COUNTER value
     */
    @Produces
    @RequestId
   open fun produceRequestId(): Int {
        return COUNTER.incrementAndGet()
    }

    /**
     * Create/get a logger instance for the class that the logger is being injected into.
     *
     * @param injectionPoint injection point
     * @return a logger instance
     */
    @Produces
    @LoggerQualifier
    open fun produceLogger(injectionPoint: InjectionPoint): Logger {
        return Logger.getLogger(injectionPoint.member.declaringClass.name)
    }

    companion object {
        private val COUNTER = AtomicInteger()
    }
}