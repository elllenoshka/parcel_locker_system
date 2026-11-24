package com.parcellocker.dpc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.parcellocker.dpc.api.DropOffRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DropOffQueue {

    private static final Logger log = LoggerFactory.getLogger(DropOffQueue.class);

    private final BlockingQueue<DropOffRequest> queue;
    private final int workers;
    private final DropOffProcessor processor;
    private final List<Thread> threads = new ArrayList<>();

    public DropOffQueue(@Value("${app.queue-capacity}") int capacity,
                        @Value("${app.workers}") int workers,
                        DropOffProcessor processor) {
        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "QUEUE_CAPACITY must be greater than 0");
        }

        if (workers < 1) {
            throw new IllegalArgumentException(
                    "WORKERS must be greater than 0");
        }
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.workers = workers;
        this.processor = processor;
    }

    @PostConstruct
    void start() {
        for (int i = 1; i <= workers; i++) {
            threads.add(Thread.ofPlatform()
                    .name("drop-worker-" + i)
                    .daemon(true)
                    .start(this::workerLoop));
        }
        log.info("started {} workers, queue capacity {}", workers, queue.remainingCapacity());
    }

    @PreDestroy
    void stop() {
        threads.forEach(Thread::interrupt);
    }

    /** Не блокує: повертає false, якщо черга повна. */
    public boolean offer(DropOffRequest request) {
        return queue.offer(request);
    }

    public boolean isFull() {
        return queue.remainingCapacity() == 0;
    }

    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                DropOffRequest request = queue.take(); // якщо черга порожня, потік чекає
                processor.process(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                log.error("worker error", e);
            }
        }
    }
}