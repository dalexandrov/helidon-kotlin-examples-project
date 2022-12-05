/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.websocket

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Class MessageQueue.
 */
class MessageQueue private constructor() {
    private val queue: Queue<String> = ConcurrentLinkedQueue()

    /**
     * Push string on stack.
     *
     * @param s String to push.
     */
    fun push(s: String) {
        queue.add(s)
    }

    /**
     * Pop string from stack.
     *
     * @return The string or `null`.
     */
    fun pop(): String {
        return queue.poll()
    }

    /**
     * Check if stack is empty.
     *
     * @return Outcome of test.
     */
    val isEmpty: Boolean
        get() = queue.isEmpty()

    /**
     * Peek at stack without changing it.
     *
     * @return String peeked or `null`.
     */
    fun peek(): String {
        return queue.peek()
    }

    companion object {
        private val INSTANCE = MessageQueue()

        /**
         * Return singleton instance of this class.
         *
         * @return Singleton.
         */
        @JvmStatic
        fun instance(): MessageQueue {
            return INSTANCE
        }
    }
}