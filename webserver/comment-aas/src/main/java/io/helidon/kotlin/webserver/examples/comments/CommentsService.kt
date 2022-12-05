/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.comments

import io.helidon.common.http.MediaType
import io.helidon.webserver.*
import single
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

/**
 * Basic service for comments.
 */
class CommentsService : Service {
    private val topicsAndComments = ConcurrentHashMap<String, MutableList<Comment>>()
    override fun update(routingRules: Routing.Rules) {
        routingRules["/{topic}", Handler { req: ServerRequest, resp: ServerResponse -> handleListComments(req, resp) }]
                .post("/{topic}", Handler { req: ServerRequest, resp: ServerResponse -> handleAddComment(req, resp) })
    }

    private fun handleListComments(req: ServerRequest, resp: ServerResponse) {
        val topic = req.path().param("topic")
        resp.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
        resp.send(listComments(topic))
    }

    private fun handleAddComment(req: ServerRequest, resp: ServerResponse) {
        val topic = req.path().param("topic")
        val userName = req.context().get("user", String::class.java)
                .orElse("anonymous")
        req.content()
                .single<String>()
                .thenAccept { msg: String? -> addComment(msg, userName, topic) }
                .thenRun { resp.send() }
                .exceptionally { t: Throwable? ->
                    req.next(t)
                    null
                }
    }

    /**
     * Adds new comment into the comment-room.
     *
     * @param message a comment message
     * @param fromUser    a user alias of the comment author
     */
    fun addComment(message: String?, fromUser: String?, toTopic: String) {
        var fromUser: String? = fromUser
        ProfanityDetector.detectProfanity(message)
        if (fromUser == null) {
            fromUser = "anonymous"
        }
        val comments = topicsAndComments.computeIfAbsent(toTopic) { Collections.synchronizedList(ArrayList()) }
        comments.add(Comment(fromUser, message))
    }

    /**
     * List all comments in original order as a string with single comment on the line.
     *
     * @param roomName a name of the room
     * @return all comments, line-by-line
     */
    fun listComments(roomName: String): String {
        val comments: List<Comment>? = topicsAndComments[roomName]
        return if (comments != null) {
            comments.stream()
                    .map { obj: Comment -> obj.toString() }
                    .collect(Collectors.joining("\n"))
        } else {
            ""
        }
    }

    private class Comment(private val userName: String?, private val message: String?) {
        override fun toString(): String {
            val result = StringBuilder()
            if (userName != null) {
                result.append(userName)
            }
            result.append(": ")
            result.append(message)
            return result.toString()
        }
    }
}

