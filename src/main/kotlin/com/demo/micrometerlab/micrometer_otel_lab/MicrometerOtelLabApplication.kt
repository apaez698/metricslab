package com.demo.micrometerlab.micrometer_otel_lab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MicrometerOtelLabApplication

fun main(args: Array<String>) {
	runApplication<MicrometerOtelLabApplication>(*args)
}
