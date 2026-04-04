package com.demo.processing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ProcessingServiceApplication

fun main(args: Array<String>) {
    runApplication<ProcessingServiceApplication>(*args)
}
