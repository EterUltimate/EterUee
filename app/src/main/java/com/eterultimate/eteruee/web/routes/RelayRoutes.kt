package com.eterultimate.eteruee.web.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import com.eterultimate.eteruee.web.dto.HttpRelayRequest
import com.eterultimate.eteruee.web.relay.HttpRelayService

fun Route.relayRoutes(
    relayService: HttpRelayService,
) {
    route("/relay") {
        post("/http") {
            val request = call.receive<HttpRelayRequest>()
            call.respond(HttpStatusCode.OK, relayService.execute(request))
        }
    }
}
