/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.security.examples.signatures

import io.helidon.common.configurable.Resource
import io.helidon.common.http.MediaType
import io.helidon.common.pki.KeyConfig
import io.helidon.security.*
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.security.providers.common.OutboundConfig
import io.helidon.security.providers.common.OutboundTarget
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider
import io.helidon.security.providers.httpauth.SecureUserStore
import io.helidon.security.providers.httpsign.HttpSignProvider
import io.helidon.security.providers.httpsign.InboundClientDefinition
import io.helidon.security.providers.httpsign.OutboundTargetDefinition
import io.helidon.webserver.*
import java.nio.file.Paths
import java.util.*

/**
 * Example of authentication of service with http signatures, using configuration file as much as possible.
 */
object SignatureExampleBuilderMain {
    private val USERS: MutableMap<String, SecureUserStore.User> = HashMap()

    // used from unit tests
    @JvmStatic
    lateinit var service1Server: WebServer
        private set

    @JvmStatic
    lateinit var service2Server: WebServer
        private set

    private fun addUser(user: String, password: String, roles: List<String>) {
        USERS[user] = object : SecureUserStore.User {
            override fun login(): String {
                return user
            }

            fun password(): CharArray {
                return password.toCharArray()
            }

            override fun isPasswordValid(password: CharArray): Boolean {
                return password().contentEquals(password)
            }

            override fun roles(): Collection<String> {
                return roles
            }
        }
    }

    /**
     * Starts this example.
     *
     * @param args ignored
     */
    @JvmStatic
    fun main(args: Array<String>?) {
        // to allow us to set host header explicitly
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true")

        // start service 2 first, as it is required by service 1
        service2Server = SignatureExampleUtil.startServer(routing2(), 9080)
        service1Server = SignatureExampleUtil.startServer(routing1(), 8080)
        println("Signature example: from builder")
        println()
        println("Users:")
        println("jack/password in roles: user, admin")
        println("jill/password in roles: user")
        println("john/password in no roles")
        println()
        println("***********************")
        println("** Endpoints:        **")
        println("***********************")
        println("Basic authentication, user role required, will use symmetric signatures for outbound:")
        System.out.printf("  http://localhost:%1\$d/service1%n", service1Server.port())
        println("Basic authentication, user role required, will use asymmetric signatures for outbound:")
        System.out.printf("  http://localhost:%1\$d/service1-rsa%n", service2Server.port())
        println()
    }

    private fun routing2(): Routing {

        // build routing (security is loaded from config)
        return Routing.builder() // helper method to load both security and web server security from configuration
                .register(WebSecurity.create(security2()).securityDefaults(WebSecurity.authenticate()))["/service2", WebSecurity.rolesAllowed("user")]["/service2-rsa", WebSecurity.rolesAllowed("user")]["/{*}", Handler { req: ServerRequest, res: ServerResponse ->
            val securityContext = req.context().get(SecurityContext::class.java)
            res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
            res.send("Response from service2, you are: \n" + securityContext
                    .flatMap { obj: SecurityContext -> obj.user() }
                    .map { obj: Subject -> obj.toString() }
                    .orElse("Security context is null") + ", service: " + securityContext
                    .flatMap { obj: SecurityContext -> obj.service() }
                    .map { obj: Subject -> obj.toString() })
        }]
                .build()
    }

    private fun routing1(): Routing {
        // build routing (security is loaded from config)
        return Routing.builder()
                .register(WebSecurity.create(security1()).securityDefaults(WebSecurity.authenticate()))["/service1", WebSecurity.rolesAllowed("user"), Handler { req: ServerRequest, res: ServerResponse -> SignatureExampleUtil.processService1Request(req, res, "/service2", service2Server.port()) }]["/service1-rsa", WebSecurity.rolesAllowed("user"), Handler { req: ServerRequest, res: ServerResponse -> SignatureExampleUtil.processService1Request(req, res, "/service2-rsa", service2Server.port()) }]
                .build()
    }

    private fun security2(): Security {
        return Security.builder()
                .providerSelectionPolicy(CompositeProviderSelectionPolicy.builder()
                        .addAuthenticationProvider("http-signatures", CompositeProviderFlag.OPTIONAL)
                        .addAuthenticationProvider("basic-auth")
                        .build())
                .addProvider(HttpBasicAuthProvider.builder()
                        .realm("mic")
                        .userStore(users()),
                        "basic-auth")
                .addProvider(HttpSignProvider.builder()
                        .addInbound(InboundClientDefinition.builder("service1-hmac")
                                .principalName("Service1 - HMAC signature")
                                .hmacSecret("somePasswordForHmacShouldBeEncrypted")
                                .build())
                        .addInbound(InboundClientDefinition.builder("service1-rsa")
                                .principalName("Service1 - RSA signature")
                                .publicKeyConfig(KeyConfig.keystoreBuilder()
                                        .keystore(Resource.create(Paths.get(
                                                "src/main/resources/keystore.p12")))
                                        .keystorePassphrase("password".toCharArray())
                                        .certAlias("service_cert")
                                        .build())
                                .build())
                        .build(),
                        "http-signatures")
                .build()
    }

    private fun security1(): Security {
        return Security.builder()
                .providerSelectionPolicy(CompositeProviderSelectionPolicy.builder()
                        .addOutboundProvider("basic-auth")
                        .addOutboundProvider("http-signatures")
                        .build())
                .addProvider(HttpBasicAuthProvider.builder()
                        .realm("mic")
                        .userStore(users())
                        .addOutboundTarget(OutboundTarget.builder("propagate-all").build()),
                        "basic-auth")
                .addProvider(HttpSignProvider.builder()
                        .outbound(OutboundConfig.builder()
                                .addTarget(hmacTarget())
                                .addTarget(rsaTarget())
                                .build()),
                        "http-signatures")
                .build()
    }

    private fun rsaTarget(): OutboundTarget {
        return OutboundTarget.builder("service2-rsa")
                .addHost("localhost")
                .addPath("/service2-rsa.*")
                .customObject(OutboundTargetDefinition::class.java,
                        OutboundTargetDefinition.builder("service1-rsa")
                                .privateKeyConfig(KeyConfig.keystoreBuilder()
                                        .keystore(Resource.create(Paths.get(
                                                "src/main/resources/keystore.p12")))
                                        .keystorePassphrase("password".toCharArray())
                                        .keyAlias("myPrivateKey")
                                        .build())
                                .build())
                .build()
    }

    private fun hmacTarget(): OutboundTarget {
        return OutboundTarget.builder("service2")
                .addHost("localhost")
                .addPath("/service2")
                .customObject(
                        OutboundTargetDefinition::class.java,
                        OutboundTargetDefinition.builder("service1-hmac")
                                .hmacSecret("somePasswordForHmacShouldBeEncrypted")
                                .build())
                .build()
    }

    private fun users(): SecureUserStore {
        return SecureUserStore { login: String -> Optional.ofNullable(USERS[login]) }
    }

    init {
        addUser("jack", "password", listOf("user", "admin"))
        addUser("jill", "password", listOf("user"))
        addUser("john", "password", listOf())
    }
}