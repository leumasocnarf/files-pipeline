package com.demo.report

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class ReportServiceApplication

fun main(args: Array<String>) {
    runApplication<ReportServiceApplication>(*args)
}
