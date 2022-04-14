package com.example.rx.weather

import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.concurrent.CopyOnWriteArraySet
import javax.servlet.http.HttpServletRequest

@RestController
class TemperatureController {

    val clients: MutableSet<SseEmitter> = CopyOnWriteArraySet()

    @RequestMapping(value = ["/temperature-stream"], method = [RequestMethod.GET])
    fun events(request: HttpServletRequest): SseEmitter {
        val emitter = SseEmitter()
        clients.add(emitter)

        emitter.onTimeout { clients.remove(emitter) }
        emitter.onCompletion { clients.remove(emitter) }
        return emitter
    }

    @Async
    @EventListener
    fun handleMessage(temperature: Temperature) {
        val deadEmitters: MutableList<SseEmitter> = ArrayList()
        clients.forEach { emitter: SseEmitter ->
            try {
                val start = Instant.now()
                emitter.send(temperature, MediaType.APPLICATION_JSON)
            } catch (ignore: Exception) {
                deadEmitters.add(emitter)
            }
        }
        clients.removeAll(deadEmitters)
    }
}
