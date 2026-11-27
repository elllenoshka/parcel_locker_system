package com.parcellocker.dpc.api;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

import com.parcellocker.dpc.model.DropOff;
import com.parcellocker.dpc.repository.DropOffRepository;
import com.parcellocker.dpc.repository.LockerCellRepository;
import com.parcellocker.dpc.service.DropOffQueue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class DropOffController {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
    private static final Set<String> SIZES = Set.of("S", "M", "L");

    private final Object admissionLock = new Object();
    private final DropOffRepository dropOffs;
    private final LockerCellRepository cells;
    private final DropOffQueue queue;

    public DropOffController(DropOffRepository dropOffs,
                             LockerCellRepository cells,
                             DropOffQueue queue) {
        this.dropOffs = dropOffs;
        this.cells = cells;
        this.queue = queue;
    }

    @PostMapping("/drop-offs")
    public ResponseEntity<DropOff> accept(@RequestBody DropOffRequest request) {
        validate(request);

        if (!cells.exists(request.lockerId(), request.size())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Locker not found",
                    "No cells of size " + request.size() + " in locker " + request.lockerId());
        }

        synchronized (admissionLock) {
            // Повторний запит із тим самим drop_id не додаємо до черги вдруге.
            var existing = dropOffs.findById(request.dropId());

            if (existing.isPresent()) {
                return accepted(existing.get());
            }

            // Відмова відбувається до створення запису в БД.
            if (queue.isFull()) {
                throw queueFull();
            }

            boolean created = dropOffs.insertQueued(request);

            DropOff dropOff = dropOffs.findById(request.dropId()).orElseThrow();

            if (created && !queue.offer(request)) {
                // За умови використання admissionLock це не повинно відбуватися.
                dropOffs.delete(request.dropId());

                throw new IllegalStateException(
                        "Queue admission invariant violated");
            }

            return accepted(dropOff);
        }
    }

    private ResponseEntity<DropOff> accepted(DropOff dropOff) {
        return ResponseEntity.accepted()
                .location(URI.create("/v1/drop-offs/" + dropOff.dropId()))
                .body(dropOff);
    }

    @GetMapping("/drop-offs/{id}")
    public DropOff get(@PathVariable("id") String id) {
        return dropOffs.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Drop-off not found", "No drop-off with id " + id));
    }

    private ApiException queueFull() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Queue is full",
                "The request queue is full, retry later", 1);
    }

    private void validate(DropOffRequest r) {
        requireId("drop_id", r.dropId());
        requireId("parcel_id", r.parcelId());
        requireId("locker_id", r.lockerId());
        if (r.size() == null || !SIZES.contains(r.size())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid request", "size must be S, M or L");
        }
    }

    private void requireId(String field, String value) {
        if (value == null || !ID.matcher(value).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid request",
                    field + " is required: 1-64 characters from A-Z a-z 0-9 _ . -");
        }
    }
}