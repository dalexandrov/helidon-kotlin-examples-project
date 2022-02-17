package support;

import io.helidon.webserver.tyrus.TyrusSupport

/**
 * DSL for the builder for Tyrus and support objects.
 */

fun tyrusSupport(block: TyrusSupport.Builder.() -> Unit = {}): TyrusSupport = TyrusSupport.builder().apply(block).build()