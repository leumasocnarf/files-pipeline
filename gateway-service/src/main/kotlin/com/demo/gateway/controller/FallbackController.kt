package com.demo.gateway.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/fallback")
class FallbackController {

    @GetMapping("/ingest")
    fun ingestFallback(): Mono<ResponseEntity<Map<String, String>>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                    mapOf(
                        "service" to "ingest-service",
                        "message" to "Ingest service is currently unavailable. Please try again later."
                    )
                )
        )
    }

    @GetMapping("/report")
    fun reportFallback(): Mono<ResponseEntity<Map<String, String>>> {
        return Mono.just(
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                    mapOf(
                        "service" to "report-service",
                        "message" to "Report service is currently unavailable. Please try again later."
                    )
                )
        )
    }
}