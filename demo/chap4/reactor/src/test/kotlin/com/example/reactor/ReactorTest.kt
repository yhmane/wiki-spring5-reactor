package com.example.reactor

import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
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

    @Test
    fun indexElements() {
        Flux.range(2018, 5) // 2018, 2019, 2020, 2021, 2022
            .timestamp()    // Flux<Tuple2<Long, Integer>> Long: timestamp, Integer: range
            .index()        // Flux<Tuple2<Long, Tuple2<Long, Integer>>> Long: index, // Long: timestamp, Integer: range
            .subscribe { e -> log.info { "index: ${e.t1}, ts:${Instant.ofEpochMilli(e.t2.t1)}, value: ${e.t2.t2}" } }
    }


    @Test
    fun startStopStreamProcessing() {
        val startCommand: Mono<*> = Mono.delay(Duration.ofSeconds(1))
        val stopCommand: Mono<*> = Mono.delay(Duration.ofSeconds(3))
        val streamOfData = Flux.interval(Duration.ofMillis(100))

        streamOfData
            .skipUntilOther(startCommand)
            .takeUntilOther(stopCommand)
            .subscribe { x -> log.info {x} }
        Thread.sleep(4000)
    }

    @Test
    fun collectSort() {
        Flux.just(1, 6, 8, 3, 1, 5, 1)
            .collectSortedList(Comparator.reverseOrder())
            .subscribe{ x -> log.info { x } }
    }

    @Test
    fun findingIfThereIsEvenElements() {
        Flux.just(3, 5, 7, 9, 11, 15, 16, 17)
            .any { e -> e % 2 == 0 }
            .subscribe {hasEvens -> log.info { "Has evens: $hasEvens" }}
    }

    @Test
    fun reduceExample() {
        Flux.range(1, 5)
            .reduce(0) { acc, elem -> acc + elem }
            .subscribe { result -> log.info {"Result: $result" } }
    }

    @Test
    fun scanExample() {
        Flux.range(1, 5)
            .scan(0) { acc, elem -> acc + elem }
            .subscribe { result -> log.info {"Result: $result" } }
    }

    @Test
    fun thenOperator() {
        Flux.just(1, 2, 3)
            .thenMany(Flux.just(4, 5))
            .subscribe {e -> log.info {"onNext: $e"} }
    }

    @Test
    fun concatExample() {
        Flux.concat(
            Flux.range(1, 3),
            Flux.range(4, 2),
            Flux.range(6, 5)
        ).subscribe {
            e -> log.info {"onNext: $e"}
        }
    }

    @Test
    fun bufferExample() {
        Flux.range(1, 13)
            .buffer(5)
            .subscribe { e -> log.info {"onNext $e"} }
    }

    @Test
    fun flatMapExample() {
        Flux.just("user-1", "user-2", "user-3")
            .flatMap { u -> requestBooks(u)
                    .map { b: String -> "$u/$b" }
            }
            .subscribe { r -> log.info {"onNext: $r" } }

        Thread.sleep(1000)
    }

    private fun requestBooks(user: String): Flux<String> {
        return Flux.range(1, Random().nextInt(3) + 1)
            .delayElements(Duration.ofMillis(3))
            .map { i: Int -> "book-$i" }
    }

    @Test
    fun sampleExample() {
        Flux.range(1, 100)
            .delayElements(Duration.ofMillis(1))
            .sample(Duration.ofMillis(20))
            .subscribe { e -> log.info {"onNext: $e"}}

        Thread.sleep(1000)
    }

    @Test
    fun doOnExample() {
        Flux.just(1, 2, 3)
            .concatWith(Flux.error(RuntimeException("Conn Error")))
            .doOnEach { s -> log.info { "signal: $s" } }
            .subscribe()
    }

    @Test
    fun signalProcessing() {
        Flux.range(1, 3)
            .doOnNext { e -> log.info {"data: $e"} }
            .materialize()
            .doOnNext { e -> log.info {"signal: $e"} }
            .dematerialize<Long>()
            .collectList()
            .subscribe { r -> log.info {"result: $r"} }
    }
}
