/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.basics

import io.helidon.common.GenericType
import io.helidon.common.http.DataChunk
import io.helidon.common.http.MediaType
import io.helidon.common.reactive.Single
import io.helidon.media.common.ContentReaders
import io.helidon.media.common.MessageBodyOperator
import io.helidon.media.common.MessageBodyReader
import io.helidon.media.common.MessageBodyReaderContext
import java.util.concurrent.Flow

@Suppress("UNCHECKED_CAST")
class NameReader private constructor() : MessageBodyReader<Name?> {
    override fun <U : Name?> read(publisher: Flow.Publisher<DataChunk>, type: GenericType<U>,
                                  context: MessageBodyReaderContext): Single<U> {
        return ContentReaders.readString(publisher, context.charset()).map { fullName: String? -> Name(fullName!!) } as Single<U>
    }

    override fun accept(type: GenericType<*>?, context: MessageBodyReaderContext): MessageBodyOperator.PredicateResult {
        return context.contentType()
                .filter { obj: MediaType? -> TYPE == obj }
                .map { MessageBodyOperator.PredicateResult.supports(Name::class.java, type) }
                .orElse(MessageBodyOperator.PredicateResult.NOT_SUPPORTED)
    }

    companion object {
        private val TYPE = MediaType.parse("application/name")
        fun create(): NameReader {
            return NameReader()
        }
    }
}