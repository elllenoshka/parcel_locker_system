package com.parcellocker.dpc.service;

import java.util.concurrent.locks.ReentrantLock;

import com.parcellocker.dpc.api.DropOffRequest;
import com.parcellocker.dpc.repository.DropOffRepository;
import com.parcellocker.dpc.repository.LockerCellRepository;
import com.parcellocker.dpc.repository.LockerCellRepository.Counter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class CellAllocator {

    private final ReentrantLock lock = new ReentrantLock();
    private final TransactionTemplate tx;
    private final LockerCellRepository cells;
    private final DropOffRepository dropOffs;

    public CellAllocator(PlatformTransactionManager txManager,
                         LockerCellRepository cells,
                         DropOffRepository dropOffs) {
        this.tx = new TransactionTemplate(txManager);
        this.cells = cells;
        this.dropOffs = dropOffs;
    }

    /** Займає комірку й ставить запиту кінцевий стан: done або rejected. */
    public void allocate(DropOffRequest request) {
        lock.lock();
        try {
            tx.executeWithoutResult(status -> {
                Counter counter = cells.findCounter(request.lockerId(), request.size())
                        .orElseThrow(() -> new IllegalStateException("locker cells not found"));

                if (counter.used() >= counter.total()) {
                    dropOffs.updateState(request.dropId(), "rejected", "no free cells");
                    return;
                }
                cells.setUsed(request.lockerId(), request.size(), counter.used() + 1);
                dropOffs.updateState(request.dropId(), "done", null);
            });
        } finally {
            lock.unlock();
        }
    }
}