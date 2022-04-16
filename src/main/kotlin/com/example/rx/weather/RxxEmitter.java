package com.example.rx.weather;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import rx.Subscriber;

class RxSeeEmitter extends SseEmitter {
    static final long SSE_SESSION_TIMEOUT = 30 * 60 * 1000L;
    private final static AtomicInteger sessionIdSequence = new AtomicInteger(0);

    private final int sessionId = sessionIdSequence.incrementAndGet();
    private final Subscriber<Temperature> subscriber;

    RxSeeEmitter() {
        super(SSE_SESSION_TIMEOUT);

        this.subscriber = new Subscriber<Temperature>() {
            @Override
            public void onNext(Temperature temperature) {
                try {
                    RxSeeEmitter.this.send(temperature);
                } catch (IOException e) {
                    unsubscribe();
                }
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onCompleted() {
            }
        };

        onCompletion(() -> {
            subscriber.unsubscribe();
        });
        onTimeout(() -> {
            subscriber.unsubscribe();
        });
    }

    Subscriber<Temperature> getSubscriber() {
        return subscriber;
    }

    int getSessionId() {
        return sessionId;
    }
}