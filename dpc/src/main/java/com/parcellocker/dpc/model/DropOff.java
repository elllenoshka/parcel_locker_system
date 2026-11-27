package com.parcellocker.dpc.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DropOff(
        @JsonProperty("drop_id") String dropId,
        @JsonProperty("parcel_id") String parcelId,
        @JsonProperty("locker_id") String lockerId,
        @JsonProperty("size") String size,
        @JsonProperty("state") String state,
        @JsonProperty("reason") String reason,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {
}
