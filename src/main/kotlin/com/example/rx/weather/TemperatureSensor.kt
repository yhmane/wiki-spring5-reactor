package com.example.rx.weather

import org.springframework.stereotype.Component
import rx.Observable
import java.util.*
import java.util.concurrent.TimeUnit


@Component
class TemperatureSensor {
    val rnd = Random()

    private val dataStream: Observable<Temperature> = Observable
        .range(0, Int.MAX_VALUE)
        .concatMap { tick -> Observable
            .just(tick)
            .delay(rnd.nextInt(5000).toLong(), TimeUnit.MILLISECONDS)
            .map { this.probe() }
        }
        .publish()
        .refCount()

    private fun probe(): Temperature = Temperature(16 + rnd.nextGaussian() * 10)

    fun temperatureStream(): Observable<Temperature> = dataStream
}
