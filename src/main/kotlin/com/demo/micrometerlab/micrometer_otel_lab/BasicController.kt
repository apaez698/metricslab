package com.demo.micrometerlab.micrometer_otel_lab

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
class BasicController(private val meterRegistry: MeterRegistry) {

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello, Micrometer + Spring Boot!"
    }

    @GetMapping("/pay")
    fun pay(@RequestParam(required = false, defaultValue = "success") status: String): String {
        val latencyMs = Random.nextLong(50, 500)
        meterRegistry.counter("payments.processed", "status", status).increment()
        meterRegistry.timer("payments.latency").record(Runnable { Thread.sleep(latencyMs) })
        return "Payment simulated: status=$status, latency=${latencyMs}ms"
    }
}