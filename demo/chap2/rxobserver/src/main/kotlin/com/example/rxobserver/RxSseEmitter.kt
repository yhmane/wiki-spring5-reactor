package com.example.rxobserver

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import rx.Subscriber;

class RxSseEmitter : SseEmitter(SSE_SESSION_TIMEOUT) {
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
                    this@RxSseEmitter.send(temperature)
                } catch (e: IOException) {
                    unsubscribe()
                }
            }

            override fun onError(e: Throwable?) {}
            override fun onCompleted() {}
        }
        onCompletion(Runnable { subscriber.unsubscribe() })
        onTimeout(Runnable { subscriber.unsubscribe() })
    }
}
