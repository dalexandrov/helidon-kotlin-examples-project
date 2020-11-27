/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package io.helidon.kotlin.examples.openapi.internal

import org.eclipse.microprofile.openapi.OASFactory
import org.eclipse.microprofile.openapi.OASModelReader
import org.eclipse.microprofile.openapi.models.OpenAPI

/**
 * Defines two paths using the OpenAPI model reader mechanism, one that should
 * be suppressed by the filter class and one that should appear in the published
 * OpenAPI document.
 */
class SimpleAPIModelReader : OASModelReader {
    override fun buildModel(): OpenAPI {
        /*
         * Add two path items, one of which we expect to be removed by
         * the filter and a very simple one that will appear in the
         * published OpenAPI document.
         */
        val newPathItem = OASFactory.createPathItem()
                .GET(OASFactory.createOperation()
                        .operationId("newPath")
                        .summary(SUMMARY))
        val doomedPathItem = OASFactory.createPathItem()
                .GET(OASFactory.createOperation()
                        .operationId(DOOMED_OPERATION_ID)
                        .summary("This should become invisible"))
        val openAPI = OASFactory.createOpenAPI()
        val paths = OASFactory.createPaths()
                .addPathItem(MODEL_READER_PATH, newPathItem)
                .addPathItem(DOOMED_PATH, doomedPathItem)
        openAPI.paths(paths)
        return openAPI
    }

    companion object {
        /**
         * Path for the example endpoint added by this model reader that should be visible.
         */
        const val MODEL_READER_PATH = "/test/newpath"

        /**
         * Path for an endpoint that the filter should hide.
         */
        const val DOOMED_PATH = "/test/doomed"

        /**
         * ID for an endpoint that the filter should hide.
         */
        const val DOOMED_OPERATION_ID = "doomedPath"

        /**
         * Summary text for the endpoint.
         */
        const val SUMMARY = "A sample test endpoint from ModelReader"
    }
}