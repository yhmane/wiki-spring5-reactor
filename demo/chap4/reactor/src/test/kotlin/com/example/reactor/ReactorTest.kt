package com.example.reactor

import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.reactivestreams.Subscription
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

private val log = KotlinLogging.logger {}

class ReactorTest {

    @Disabled("testIsEndless")
    @Test
    fun endlessFlux() {
        Flux.range(1, 5)
            .repeat()
            .collectList()
            .block()
    }

    @Test
    fun createFluxStream() {
        val fluxStream0 = Flux.just("Hello")
        val fluxStream1 = Flux.just("Hello", "World", "!!")
        val fluxStream2 = Flux.fromArray(arrayOf(1, 2, 3))
        val fluxStream3 = Flux.fromIterable(listOf(9, 8, 7))
        val fluxStream4 = Flux.range(2010, 9)
        val fluxStream5 = Flux.empty<String>()
        val fluxStream6 = Flux.never<String>()

        log.info { "fluxStream0: $fluxStream0" } // FluxJust
        log.info { "fluxStream1: $fluxStream1" } // FluxArray
        log.info { "fluxStream2: $fluxStream2" } // FluxArray
        log.info { "fluxStream3: $fluxStream3" } // FluxIterable
        log.info { "fluxStream4: $fluxStream4" } // FluxRange
        log.info { "fluxStream5: $fluxStream5" } // FluxEmpty
        log.info { "fluxStream6: $fluxStream6" } // FluxNever
    }

    @Test
    fun createMonoStream() {
        val monoStream0 = Mono.just("one")
        val monoStream1 = Mono.justOrEmpty<String>(null)
        val monoStream2 = Mono.justOrEmpty<String>(Optional.empty())
        val monoStream3 = Mono.fromCallable { httpRequest() }
        val monoStream4: Mono<String>? = Mono.error(RuntimeException("unknown Id"))

        log.info { "monoStream0: $monoStream0" } // MonoJust
        log.info { "monoStream1: $monoStream1" } // MonoEmpty
        log.info { "monoStream2: $monoStream2" } // MonoEmpty
        log.info { "monoStream3: $monoStream3" } // MonoCallable
        log.info { "monoStream4: $monoStream4" } // MonoCallable
    }

    private fun httpRequest(): String {
        log.info("Making HTTP request")
        throw RuntimeException("IO error")
    }

    @Test
    fun simpleSunbscribe() {
        Flux.just("A", "B", "C")
            .subscribe(
                { data -> log.info { "onNext: $data"} },  // consumer
                { }, // errorConsumer
                { log.info { "onComplete"} } // completeConsumer
            )
    }

    @Test
    fun managingDemand() {
        Flux.range(1, 100)
            .subscribe(
                { data -> log.info { "onNext: $data"} },  // consumer
                { }, // errorConsumer
                { log.info { "onComplete"} }, // completeConsumer
            )
            { subscription ->
                subscription.request(40)
                subscription.cancel()
            }
    }

    @Test
    fun managingSubscription() {
        val disposable: Disposable = Flux.interval(Duration.ofMillis(30))
            .subscribe { data -> log.info { "onNext: $data" } }

        Thread.sleep(200)
        disposable.dispose()
    }
}
