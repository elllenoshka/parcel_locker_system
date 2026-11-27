package com.parcellocker.dpc.api;

import java.util.List;

import com.parcellocker.dpc.repository.LockerCellRepository;
import com.parcellocker.dpc.repository.LockerCellRepository.CellRow;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class LockerCellController {

    private final LockerCellRepository cells;

    public LockerCellController(LockerCellRepository cells) {
        this.cells = cells;
    }

    @GetMapping("/locker-cells/{lockerId}")
    public LockerCellsResponse get(@PathVariable("lockerId") String lockerId) {
        List<CellRow> rows = cells.findByLocker(lockerId);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Locker not found", "No locker " + lockerId);
        }

        List<CellState> states = rows.stream()
                .map(r -> new CellState(r.size(), r.total(), r.used(), r.total() - r.used(), r.done()))
                .toList();

        // Інваріант: used не більше за total і дорівнює кількості закладань у стані done
        boolean invariantOk = rows.stream()
                .allMatch(r -> r.used() >= 0 && r.used() <= r.total() && r.used() == r.done());

        return new LockerCellsResponse(lockerId, states, invariantOk);
    }

}