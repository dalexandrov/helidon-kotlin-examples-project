/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.abac

import io.helidon.security.*
import io.helidon.security.spi.AuthenticationProvider
import io.helidon.security.spi.SynchronousProvider
import java.lang.annotation.Inherited
import java.util.*
import java.util.Set
import java.util.function.Consumer

/**
 * Example authentication provider that reads annotation to create a subject.
 */
open class AtnProvider : SynchronousProvider(), AuthenticationProvider {
    override fun syncAuthenticate(providerRequest: ProviderRequest): AuthenticationResponse {
        val securityLevels = providerRequest.endpointConfig().securityLevels()
        val listIterator: ListIterator<SecurityLevel> = securityLevels.listIterator(securityLevels.size)
        var user: Subject? = null
        var service: Subject? = null
        while (listIterator.hasPrevious()) {
            val securityLevel = listIterator.previous()
            val authenticationAnnots = securityLevel
                    .filterAnnotations(Authentications::class.java, EndpointConfig.AnnotationScope.METHOD)
            val authentications: MutableList<Authentication> = LinkedList()
            authenticationAnnots.forEach(Consumer { atn: Authentications -> authentications.addAll(listOf(*atn.value)) })
            if (authentications.isNotEmpty()) {
                for (authentication in authentications) {
                    if (authentication.type == SubjectType.USER) {
                        user = buildSubject(authentication)
                    } else {
                        service = buildSubject(authentication)
                    }
                }
                break
            }
        }
        return AuthenticationResponse.success(user, service)
    }

    private fun buildSubject(authentication: Authentication): Subject {
        val subjectBuilder = Subject.builder()
        subjectBuilder.principal(Principal.create(authentication.value))
        for (role in authentication.roles) {
            subjectBuilder.addGrant(Role.create(role))
        }
        for (scope in authentication.scopes) {
            subjectBuilder.addGrant(Grant.builder()
                    .name(scope)
                    .type("scope")
                    .build())
        }
        return subjectBuilder.build()
    }

    override fun supportedAnnotations(): Collection<Class<out Annotation?>> {
        return Set.of<Class<out Annotation?>>(Authentication::class.java)
    }

    /**
     * Authentication annotation.
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @MustBeDocumented
    @Inherited
    @Repeatable
    annotation class Authentication(
            /**
             * Name of the principal.
             *
             * @return principal name
             */
            val value: String,
            /**
             * Type of the subject, defaults to user.
             *
             * @return type
             */
            val type: SubjectType = SubjectType.USER,
            /**
             * Granted roles.
             * @return array of roles
             */
            val roles: Array<String> = [""],
            /**
             * Granted scopes.
             * @return array of scopes
             */
            val scopes: Array<String> = [""])

    /**
     * Repeatable annotation for [Authentication].
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @MustBeDocumented
    @Inherited
    annotation class Authentications(
            /**
             * Repeating annotation.
             * @return annotations
             */
            vararg val value: Authentication)
}