package com.demo.ingest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IngestServiceApplication

fun main(args: Array<String>) {
    runApplication<IngestServiceApplication>(*args)
}
