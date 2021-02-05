/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.faulttolerance

import io.helidon.common.reactive.Single
import io.helidon.faulttolerance.*
import io.helidon.webserver.*
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple service to demonstrate fault tolerance.
 */
class FtService internal constructor() : Service {
    private val async: Async = Async.create()
    private val bulkhead: Bulkhead = Bulkhead.builder()
            .queueLength(1)
            .limit(1)
            .name("helidon-example-bulkhead")
            .build()
    private val breaker: CircuitBreaker = CircuitBreaker.builder()
            .volume(4)
            .errorRatio(40)
            .successThreshold(1)
            .delay(Duration.ofSeconds(5))
            .build()
    private val fallback: Fallback<String?>
    private val retry: Retry
    private val timeout: Timeout
    override fun update(rules: Routing.Rules) {
        rules["/async", Handler { _: ServerRequest, response: ServerResponse -> asyncHandler(response) }]["/bulkhead/{millis}", Handler { request: ServerRequest, response: ServerResponse -> bulkheadHandler(request, response) }]["/circuitBreaker/{success}", Handler { request: ServerRequest, response: ServerResponse -> circuitBreakerHandler(request, response) }]["/fallback/{success}", Handler { request: ServerRequest, response: ServerResponse -> fallbackHandler(request, response) }]["/retry/{count}", Handler { request: ServerRequest, response: ServerResponse -> retryHandler(request, response) }]["/timeout/{millis}", Handler { request: ServerRequest, response: ServerResponse -> timeoutHandler(request, response) }]
    }

    private fun timeoutHandler(request: ServerRequest, response: ServerResponse) {
        val sleep = request.path().param("millis").toLong()
        timeout.invoke { sleep(sleep) }
                .thenAccept { t: String? -> response.send(t) }
                .exceptionally { throwable: Throwable? -> response.send(throwable) }
    }

    private fun retryHandler(request: ServerRequest, response: ServerResponse) {
        val count = request.path().param("count").toInt()
        val call = AtomicInteger(1)
        val failures = AtomicInteger()
        retry.invoke {
            val current = call.getAndIncrement()
            if (current < count) {
                failures.incrementAndGet()
                return@invoke reactiveFailure()
            }
            Single.just("calls/failures: " + current + "/" + failures.get())
        }.thenAccept { t: String? -> response.send(t) }
                .exceptionally { throwable: Throwable? -> response.send(throwable) }
    }

    private fun fallbackHandler(request: ServerRequest, response: ServerResponse) {
        val success = "true".equals(request.path().param("success"), ignoreCase = true)
        if (success) {
            fallback.invoke { reactiveData() }.thenAccept { t: String? -> response.send(t) }
        } else {
            fallback.invoke { reactiveFailure() }.thenAccept { t: String? -> response.send(t) }
        }
    }

    private fun circuitBreakerHandler(request: ServerRequest, response: ServerResponse) {
        val success = "true".equals(request.path().param("success"), ignoreCase = true)
        if (success) {
            breaker.invoke { reactiveData() }
                    .thenAccept { t: String? -> response.send(t) }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        } else {
            breaker.invoke { reactiveFailure() }
                    .thenAccept { t: String? -> response.send(t) }
                    .exceptionally { throwable: Throwable? -> response.send(throwable) }
        }
    }

    private fun bulkheadHandler(request: ServerRequest, response: ServerResponse) {
        val sleep = request.path().param("millis").toLong()
        bulkhead.invoke { sleep(sleep) }
                .thenAccept { t: String? -> response.send(t) }
                .exceptionally { throwable: Throwable? -> response.send(throwable) }
    }

    private fun asyncHandler(response: ServerResponse) {
        async.invoke { blockingData() }.thenApply { t: String? -> response.send(t) }
    }

    private fun reactiveFailure(): Single<String?> {
        return Single.error(RuntimeException("reactive failure"))
    }

    private fun sleep(sleepMillis: Long): Single<String?> {
        return async.invoke {
            try {
                Thread.sleep(sleepMillis)
            } catch (ignored: InterruptedException) {
            }
            "Slept for $sleepMillis ms"
        }
    }

    private fun reactiveData(): Single<String?> {
        return async.invoke { blockingData() }
    }

    private fun blockingData(): String {
        try {
            Thread.sleep(100)
        } catch (ignored: InterruptedException) {
        }
        return "blocked for 100 millis"
    }

    private fun fallbackToMethod(e: Throwable): Single<String?> {
        return Single.just("Failed back because of " + e.message)
    }

    init {
        fallback = Fallback.create { e: Throwable -> fallbackToMethod(e) }
        retry = Retry.builder()
                .retryPolicy(Retry.DelayingRetryPolicy.noDelay(3))
                .build()
        timeout = Timeout.create(Duration.ofMillis(100))
    }
}