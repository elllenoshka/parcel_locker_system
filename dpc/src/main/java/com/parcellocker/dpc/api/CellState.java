package com.parcellocker.dpc.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CellState(
        @JsonProperty("size") String size,
        @JsonProperty("total") int total,
        @JsonProperty("used") int used,
        @JsonProperty("free") int free,
        @JsonProperty("done") int done) {
}