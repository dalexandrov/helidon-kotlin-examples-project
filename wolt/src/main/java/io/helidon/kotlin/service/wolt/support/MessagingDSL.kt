package support;

import io.helidon.messaging.Channel
import io.helidon.messaging.Messaging

/**
 * DSL for the builder for Messaging and support objects.
 */

fun messaging(block: Messaging.Builder.() -> Unit = {}): Messaging = Messaging.builder().apply(block).build()
fun <T> channel(block:  Channel.Builder<T>.() -> Unit = {}): Channel<T> = Channel.builder<T>().apply(block).build()