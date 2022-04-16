package com.example.rx.weather

import mu.KotlinLogging
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import rx.Subscriber
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
class TemperatureController(
    private val temperatureSensor: TemperatureSensor,
) {


    @RequestMapping(value = ["/temperature-stream"], method = [RequestMethod.GET])
    fun events(request: HttpServletRequest): SseEmitter {
        val emitter = RxSeeEmitter()
        temperatureSensor.temperatureStream()
            .subscribe(emitter.subscriber)
        return emitter
    }

    internal class RxSeeEmitter : SseEmitter(SSE_SESSION_TIMEOUT) {
        val sessionId = sessionIdSequence.incrementAndGet()

        val subscriber: Subscriber<Temperature>

        companion object {
            const val SSE_SESSION_TIMEOUT = 30 * 60 * 1000L
            private val sessionIdSequence = AtomicInteger(0)
        }

        init {
            subscriber = object : Subscriber<Temperature>() {
                override fun onNext(temperature: Temperature) {
                    try {
                        this@RxSeeEmitter.send(temperature)
                        log.info("[{}] << {} ", sessionId, temperature.value)
                    } catch (e: IOException) {
                        log.warn("[{}] Can not send event to SSE, closing subscription, message: {}",
                            sessionId, e.message)
                        unsubscribe()
                    }
                }

                override fun onError(e: Throwable) {
                    log.warn("[{}] Received sensor error: {}", sessionId, e.message)
                }

                override fun onCompleted() {
                    log.warn("[{}] Stream completed", sessionId)
                }
            }
            onCompletion {
                log.info("[{}] SSE completed", sessionId)
                subscriber.unsubscribe()
            }
            onTimeout {
                log.info("[{}] SSE timeout", sessionId)
                subscriber.unsubscribe()
            }
        }
    }

}
