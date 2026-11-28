package com.parcellocker.dpc.service;

import java.util.concurrent.ThreadLocalRandom;

import com.parcellocker.dpc.api.DropOffRequest;
import com.parcellocker.dpc.repository.DropOffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DropOffProcessor {

    private static final Logger log = LoggerFactory.getLogger(DropOffProcessor.class);

    private final DropOffRepository dropOffs;
    private final CellAllocator allocator;
    private final long slowMinMs;
    private final long slowMaxMs;

    public DropOffProcessor(DropOffRepository dropOffs,
                            CellAllocator allocator,
                            @Value("${app.slow-call-min-ms}") long slowMinMs,
                            @Value("${app.slow-call-max-ms}") long slowMaxMs) {
        this.dropOffs = dropOffs;
        this.allocator = allocator;
        this.slowMinMs = slowMinMs;
        this.slowMaxMs = slowMaxMs;
    }

    public void process(DropOffRequest request) {
        try {
            dropOffs.updateState(request.dropId(), "processing", null);
            registerShipment();          // повільний виклик, блокування не тримаємо
            allocator.allocate(request); // критична секція всередині
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(request, "interrupted");
        } catch (RuntimeException e) {
            log.error("drop-off {} failed", request.dropId(), e);
            fail(request, e.getMessage());
        }
    }

    /** Імітація реєстрації відправлення в службі доставки: 200-600 мс. */
    private void registerShipment() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextLong(slowMinMs, slowMaxMs + 1));
    }

    private void fail(DropOffRequest request, String reason) {
        try {
            dropOffs.updateState(request.dropId(), "failed", reason);
        } catch (RuntimeException e) {
            log.error("cannot mark drop-off {} as failed", request.dropId(), e);
        }
    }
}