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
 */
package io.helidon.kotlin.security.examples.abac

import io.helidon.security.SecurityContext
import io.helidon.security.abac.policy.PolicyValidator
import io.helidon.security.abac.scope.ScopeValidator
import io.helidon.security.abac.time.TimeValidator
import io.helidon.security.annotations.Authenticated
import io.helidon.security.annotations.Authorized
import io.helidon.kotlin.security.examples.abac.AtnProvider.Authentication
import java.time.DayOfWeek
import javax.json.JsonString
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Explicit authorization resource - authorization must be called by programmer.
 */
@Path("/explicit")
@TimeValidator.TimeOfDay(from = "08:15:00", to = "12:00:00")
//@TimeValidator.TimeOfDay(from = "12:30:00", to = "17:30:00") !NOT SUPPORTED IN KOTLIN!
@TimeValidator.DaysOfWeek(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
@ScopeValidator.Scope("calendar_read")
//@ScopeValidator.Scope("calendar_edit")!NOT SUPPORTED IN KOTLIN!
@PolicyValidator.PolicyStatement("\${env.time.year >= 2017 && object.owner == subject.principal.id}")
@Authenticated
open class AbacExplicitResource {
    /**
     * A resource method to demonstrate explicit authorization.
     *
     * @param context  security context (injected)
     * @return "fine, sir" string; or a description of authorization failure
     */
    @GET
    @Authorized(explicit = true)
    @Authentication(value = "user", roles = ["user_role"], scopes = ["calendar_read", "calendar_edit"])
    //@Authentication(value = "service", type = SubjectType.SERVICE, roles = ["service_role"], scopes = ["calendar_read", "calendar_edit"]) !NOT SUPPORTED IN KOTLIN!
    fun process(@Context context: SecurityContext): Response {
        val res = SomeResource("user")
        val atzResponse = context.authorize(res)
        return if (atzResponse.isPermitted) {
            //do the update
            Response.ok().entity("fine, sir").build()
        } else {
            Response.status(Response.Status.FORBIDDEN)
                    .entity(atzResponse.description().orElse("Access not granted"))
                    .build()
        }
    }

    /**
     * A resource method to demonstrate explicit authorization - this should fail, as we do not call authorization.
     *
     * @param context security context (injected)
     * @param object a JSON string
     * @return "fine, sir" string; or a description of authorization failure
     */
    @POST
    @Path("/deny")
    @Authorized(explicit = true)
    @Authentication(value = "user", roles = ["user_role"], scopes = ["calendar_read", "calendar_edit"])
    //@Authentication(value = "service", type = SubjectType.SERVICE, roles = ["service_role"], scopes = ["calendar_read", "calendar_edit"]) !NOT SUPPORTED IN KOTLIN!
    @Consumes(MediaType.APPLICATION_JSON)
    fun fail(@Context context: SecurityContext?, `object`: JsonString?): Response {
        return Response.ok("This should not work").build()
    }

    /**
     * Example resource.
     */
    class SomeResource internal constructor(var owner: String) {
        var id = "id"
        var message = "Unit test"
    }
}