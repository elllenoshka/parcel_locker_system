package com.parcellocker.dpc.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LockerCellsResponse(
        @JsonProperty("locker_id") String lockerId,
        @JsonProperty("cells") List<CellState> cells,
        @JsonProperty("invariant_ok") boolean invariantOk) {
}