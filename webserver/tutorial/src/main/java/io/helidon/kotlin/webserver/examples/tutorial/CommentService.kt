/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates. All rights reserved.
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
@file:Suppress("UNCHECKED_CAST")

package io.helidon.kotlin.webserver.examples.tutorial

import io.helidon.common.http.DataChunk
import io.helidon.common.http.MediaType
import io.helidon.kotlin.webserver.examples.tutorial.user.User
import io.helidon.media.common.ContentWriters
import io.helidon.webserver.*
import single
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Flow
import java.util.stream.Collectors

/**
 * Basic service for comments.
 */
class CommentService : Service {
    private val data = ConcurrentHashMap<String, MutableList<Comment>>()
    override fun update(routingRules: Routing.Rules) {
        routingRules[Handler { req: ServerRequest, res: ServerResponse ->
            // Register a publisher for comment
            res.registerWriter(MutableList::class.java, MediaType.TEXT_PLAIN.withCharset("UTF-8")
            ) { comments: List<*> -> publish(comments as List<Comment>) }
            req.next()
        }]["/{" + ROOM_PATH_ID + "}", Handler { req: ServerRequest, resp: ServerResponse -> this.getComments(req, resp) }]
                .post("/{" + ROOM_PATH_ID + "}", Handler { req: ServerRequest, resp: ServerResponse -> this.addComment(req, resp) })
    }

    fun publish(comments: List<Comment>): Flow.Publisher<DataChunk?> {
        val str = comments.stream()
                .map { obj: Comment -> obj.toString() }
                .collect(Collectors.joining("\n"))
        return ContentWriters.charSequenceWriter(StandardCharsets.UTF_8)
                .apply("""
    $str
    
    """.trimIndent())
    }

    private fun getComments(req: ServerRequest, resp: ServerResponse) {
        val roomId = req.path().param(ROOM_PATH_ID)
        //resp.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"));
        val comments = getComments(roomId)
        resp.send(comments)
    }

    /**
     * Returns all comments for the room or an empty list if room doesn't exist.
     *
     * @param roomId a room ID
     * @return a list of comments
     */
    fun getComments(roomId: String?): List<Comment> {
        if (roomId == null || roomId.isEmpty()) {
            return emptyList()
        }
        val result: List<Comment>? = data[roomId]
        return result ?: emptyList()
    }

    private fun addComment(req: ServerRequest, resp: ServerResponse) {
        val roomId = req.path().param(ROOM_PATH_ID)
        val user = req.context()
                .get(User::class.java)
                .orElse(User.ANONYMOUS)
        req.content()
                .single<String>()
                .thenAccept { msg: String? -> addComment(roomId, user, msg) }
                .thenRun { resp.send() }
                .exceptionally { t: Throwable? ->
                    req.next(t)
                    null
                }
    }

    /**
     * Adds new comment into the comment-room.
     *
     * @param roomName a name of the comment-room
     * @param user     a user who provides the comment
     * @param message  a comment message
     */
    fun addComment(roomName: String, user: User?, message: String?) {
        var user = user
        if (user == null) {
            user = User.ANONYMOUS
        }
        val comments = data.computeIfAbsent(roomName) { Collections.synchronizedList(ArrayList()) }
        comments.add(Comment(user, message))
    }

    /**
     * Represents a single comment.
     */
    class Comment internal constructor(private val user: User?, private val message: String?) {
        override fun toString(): String {
            val result = StringBuilder()
            if (user != null) {
                result.append(user.alias)
            }
            result.append(": ")
            result.append(message)
            return result.toString()
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o !is Comment) {
                return false
            }
            if (if (user != null) user != o.user else o.user != null) {
                return false
            }
            return if (message != null) message == o.message else o.message == null
        }

        override fun hashCode(): Int {
            var result = user?.hashCode() ?: 0
            result = 31 * result + (message?.hashCode() ?: 0)
            return result
        }
    }

    companion object {
        private const val ROOM_PATH_ID = "room-id"
    }
}