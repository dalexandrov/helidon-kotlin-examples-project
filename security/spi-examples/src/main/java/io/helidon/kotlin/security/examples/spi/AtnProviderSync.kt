/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.spi

import asType
import io.helidon.config.Config
import io.helidon.security.*
import io.helidon.security.spi.AuthenticationProvider
import io.helidon.security.spi.SynchronousProvider
import java.util.*

/**
 * Example of an authentication provider implementation - synchronous.
 * This is a full-blows example of a provider that requires additional configuration on a resource.
 */
class AtnProviderSync : SynchronousProvider(), AuthenticationProvider {
    public override fun syncAuthenticate(providerRequest: ProviderRequest): AuthenticationResponse {

        // first obtain the configuration of this request
        // either from annotation, custom object or config
        val myObject = getCustomObject(providerRequest.endpointConfig())
            ?: // I do not have my required information, this request is probably not for me
            return AuthenticationResponse.abstain()
        return if (myObject.isValid) {
            // now authenticate - this example just creates a subject
            // based on the value (user subject) and size (group subject)
            AuthenticationResponse.success(
                Subject.builder()
                    .addPrincipal(Principal.create(myObject.value))
                    .addGrant(Role.create("role_" + myObject.size))
                    .build()
            )
        } else {
            AuthenticationResponse.failed("Invalid request")
        }
    }

    private fun getCustomObject(epConfig: EndpointConfig): AtnObject? {
        // order I choose - this depends on type of security you implement and your choice:
        // 1) custom object in request (as this must be explicitly done by a developer)
        var opt: Optional<out AtnObject?> = epConfig.instance(AtnObject::class.java)
        if (opt.isPresent) {
            return opt.get()
        }

        // 2) configuration in request
        opt = epConfig.config("atn-object")
            .flatMap { conf: Config -> conf.asType() { config: Config -> AtnObject.from(config) }.asOptional() }
        if (opt.isPresent) {
            return opt.get()
        }

        // 3) annotations on target
        val annots: MutableList<AtnAnnot> = ArrayList()
        for (securityLevel in epConfig.securityLevels()) {
            annots.addAll(securityLevel.combineAnnotations(AtnAnnot::class.java, *EndpointConfig.AnnotationScope.values()))
        }
        return if (annots.isEmpty()) {
            null
        } else {
            AtnObject.from(annots[0])
        }
    }

    override fun supportedAnnotations(): Collection<Class<out Annotation?>> {
        return mutableSetOf<Class<out Annotation?>>(AtnAnnot::class.java)
    }

    /**
     * This is an example annotation to see how to work with them.
     */
    @kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.CLASS,
        AnnotationTarget.FIELD
    )
    @MustBeDocumented
    annotation class AtnAnnot(
        /**
         * This is an example value.
         *
         * @return some value
         */
        val value: String,
        /**
         * This is an example value.
         *
         * @return some size
         */
        val size: Int = 4
    )

    /**
     * This is an example custom object.
     * Also acts as an object to get configuration in config.
     */
    class AtnObject {
        var value: String? = null
        var size = 4
        val isValid: Boolean
            get() = null != value

        companion object {
            /**
             * Load this object instance from configuration.
             *
             * @param config configuration
             * @return a new instance
             */
            fun from(config: Config): AtnObject {
                val result = AtnObject()
                config["value"].asString().ifPresent { value: String? -> result.value = value }
                config["size"].asInt().ifPresent { size: Int -> result.size = size }
                return result
            }

            fun from(annot: AtnAnnot): AtnObject {
                val result = AtnObject()
                result.value = annot.value
                result.size = annot.size
                return result
            }

            @JvmStatic
            fun from(value: String?, size: Int): AtnObject {
                val result = AtnObject()
                result.value = value
                result.size = size
                return result
            }
        }
    }
}