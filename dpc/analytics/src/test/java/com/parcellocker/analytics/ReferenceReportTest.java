package com.parcellocker.analytics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceReportTest {

    private static final Path FIXTURES = Path.of("fixtures");

    @Test
    void returnsSevenGroupsAndExactCounts() throws IOException {
        var result = ReferenceReport.calculate(
                FIXTURES.resolve("history.csv"), FIXTURES.resolve("lockers.csv"));
        assertEquals(7, result.size());
        assertEquals(8, result.stream().mapToLong(ReferenceReport.ReportRow::parcelCount).sum());
        assertEquals(6, result.stream().mapToLong(ReferenceReport.ReportRow::pickedCount).sum());
        Map<String, ReferenceReport.ReportRow> byKey = result.stream().collect(
                Collectors.toMap(
                        r -> r.lockerId() + ":" + r.day() + ":" + r.size(),
                        Function.identity()));
        var first = byKey.get("locker-0001:2026-11-20:M");
        assertEquals(2, first.parcelCount());
        assertEquals(1, first.pickedCount());
        assertEquals(0, byKey.get("locker-0002:2026-11-21:L").pickedCount());
    }

    @Test
    void rejectsUnknownLocker(@TempDir Path temp) throws IOException {
        Path invalid = temp.resolve("invalid.csv");
        Files.writeString(invalid,
                "parcel_id,locker_id,size,dropped,picked,sender_id\n" +
                "p1,does-not-exist,S,2026-11-20T09:00:00Z,,sender-1\n");
        var error = assertThrows(IllegalArgumentException.class,
                () -> ReferenceReport.calculate(invalid, FIXTURES.resolve("lockers.csv")));
        assertTrue(error.getMessage().contains("Unknown locker"));
    }

    @Test
    void rejectsDuplicateParcel(@TempDir Path temp) throws IOException {
        Path invalid = temp.resolve("duplicate.csv");
        String row = "p1,locker-0001,S,2026-11-20T09:00:00Z,,sender-1\n";
        Files.writeString(invalid,
                "parcel_id,locker_id,size,dropped,picked,sender_id\n" + row + row);
        var error = assertThrows(IllegalArgumentException.class,
                () -> ReferenceReport.calculate(invalid, FIXTURES.resolve("lockers.csv")));
        assertTrue(error.getMessage().contains("Duplicate parcel"));
    }
}
