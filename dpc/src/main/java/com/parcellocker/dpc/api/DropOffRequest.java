package com.parcellocker.dpc.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DropOffRequest(
        @JsonProperty("drop_id") String dropId,
        @JsonProperty("parcel_id") String parcelId,
        @JsonProperty("locker_id") String lockerId,
        @JsonProperty("size") String size) {
}