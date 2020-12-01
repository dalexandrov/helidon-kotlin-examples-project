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
package io.helidon.kotlin.security.examples.webserver.digest

import io.helidon.common.http.MediaType
import io.helidon.security.Security
import io.helidon.security.SecurityContext
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.security.providers.httpauth.HttpDigest
import io.helidon.security.providers.httpauth.HttpDigestAuthProvider
import io.helidon.security.providers.httpauth.SecureUserStore
import io.helidon.webserver.*
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.logging.LogManager
import kotlin.experimental.and

/**
 * Example of HTTP digest authentication with WebServer fully configured programmatically.
 */
object DigestExampleBuilderMain {
    // used from unit tests
    @JvmStatic
    lateinit var server: WebServer
        private set

    // simple approach to user storage - for real world, use data store...
    private val users: MutableMap<String, MyUser> = HashMap()
    private val HEX_ARRAY = "0123456789abcdef".toCharArray()

    /**
     * Starts this example. Programmatical configuration. See standard output for instructions.
     *
     * @param args ignored
     * @throws IOException in case of logging configuration failure
     */
    @JvmStatic
    @Throws(IOException::class)
    fun main(args: Array<String>) {
        // load logging configuration
        LogManager.getLogManager().readConfiguration(DigestExampleConfigMain::class.java.getResourceAsStream("/logging.properties"))

        // build routing (same as done in application.conf)
        val routing = Routing.builder()
                .register(buildWebSecurity().securityDefaults(WebSecurity.authenticate()))["/noRoles", WebSecurity.enforce()]["/user[/{*}]", WebSecurity.rolesAllowed("user")]["/admin", WebSecurity.rolesAllowed("admin")]["/deny", WebSecurity.rolesAllowed("deny").audit()] // roles allowed imply authn and authz
                .any("/noAuthn", WebSecurity.rolesAllowed("admin")
                        .authenticationOptional()
                        .audit())["/{*}", Handler { req: ServerRequest, res: ServerResponse ->
            val securityContext = req.context().get(SecurityContext::class.java)
            res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
            res.send("Hello, you are: \n" + securityContext
                    .map { ctx: SecurityContext -> ctx.user().orElse(SecurityContext.ANONYMOUS).toString() }
                    .orElse("Security context is null"))
        }]
                .build()

        // start server (blocks until started)
        server = DigestExampleUtil.startServer(routing)
    }

    private fun buildWebSecurity(): WebSecurity {
        val security = Security.builder()
                .addAuthenticationProvider(
                        HttpDigestAuthProvider.builder()
                                .realm("mic")
                                .digestServerSecret("aPassword".toCharArray())
                                .userStore(buildUserStore()),
                        "digest-auth")
                .build()
        return WebSecurity.create(security)
    }

    private fun buildUserStore(): SecureUserStore {
        return SecureUserStore { login: String -> Optional.ofNullable(users[login]) }
    }

    private class MyUser(private val login: String, private val password: CharArray, private val roles: Set<String>) : SecureUserStore.User {
        private fun password(): CharArray {
            return password
        }

        override fun isPasswordValid(password: CharArray): Boolean {
            return password().contentEquals(password)
        }

        override fun digestHa1(realm: String, algorithm: HttpDigest.Algorithm): Optional<String> {
            require(algorithm == HttpDigest.Algorithm.MD5) { "Unsupported algorithm $algorithm" }
            val a1 = login + ":" + realm + ":" + String(password())
            val bytes = a1.toByteArray(StandardCharsets.UTF_8)
            val digest: MessageDigest
            digest = try {
                MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException("MD5 algorithm should be supported", e)
            }
            return Optional.of(bytesToHex(digest.digest(bytes)))
        }

        override fun roles(): Set<String> {
            return roles
        }

        override fun login(): String {
            return login
        }

        companion object {
            private fun bytesToHex(bytes: ByteArray): String {
                val hexChars = CharArray(bytes.size * 2)
                for (j in bytes.indices) {
                    val v: Byte = bytes[j] and 0xFF.toByte()
                    hexChars[j * 2] = HEX_ARRAY[v.toInt() % 4]
                    hexChars[j * 2 + 1] = HEX_ARRAY[(v and 0x0F).toInt()]
                }
                return String(hexChars)
            }
        }
    }

    init {
        users["jack"] = MyUser("jack", "password".toCharArray(), mutableSetOf("user", "admin"))
        users["jill"] = MyUser("jill", "password".toCharArray(), mutableSetOf("user"))
        users["john"] = MyUser("john", "password".toCharArray(), mutableSetOf())
    }
}
