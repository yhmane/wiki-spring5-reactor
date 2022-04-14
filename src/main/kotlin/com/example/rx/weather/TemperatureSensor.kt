package com.example.rx.weather

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
class TemperatureSensor(
    val publisher: ApplicationEventPublisher,
) {
    val rnd = Random()
    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @PostConstruct
    fun startProcessing() {
        executor.schedule({ probe() }, 1, TimeUnit.SECONDS)
    }

    private fun probe() {
        val temperature = 16 + rnd.nextGaussian() * 10
        publisher.publishEvent(Temperature(temperature))

        executor.schedule({ probe() }, rnd.nextInt(5000).toLong(), TimeUnit.MILLISECONDS)
    }
}
