package com.parcellocker.analytics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O4.3: independent batch report reference. Does not use Spark.
 * For the small fixtures only: commas and quoted CSV fields are not supported.
 * "day" is the UTC date of drop-off (team's provisional reporting convention).
 */
public final class ReferenceReport {

    private ReferenceReport() {}

    public record ReportRow(
            String lockerId, LocalDate day, String size, String address,
            long parcelCount, long pickedCount) {}

    private record GroupKey(String lockerId, LocalDate day, String size) {}

    private static List<String[]> readCsv(Path file, String expectedHeader, int columns)
            throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.getFirst().equals(expectedHeader)) {
            throw new IllegalArgumentException("Unexpected CSV header in " + file);
        }
        List<String[]> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            String[] parts = lines.get(i).split(",", -1);
            if (parts.length != columns) {
                throw new IllegalArgumentException("Bad CSV row at " + file + ":" + (i + 1));
            }
            result.add(parts);
        }
        return result;
    }

    public static List<ReportRow> calculate(Path history, Path lockers) throws IOException {
        Map<String, String> addresses = new HashMap<>();
        for (String[] row : readCsv(lockers, "locker_id,address,tz", 3)) {
            if (addresses.putIfAbsent(row[0], row[1]) != null) {
                throw new IllegalArgumentException("Duplicate locker: " + row[0]);
            }
        }

        Map<GroupKey, long[]> groups = new HashMap<>();
        Set<String> seenParcels = new HashSet<>();
        for (String[] row : readCsv(
                history, "parcel_id,locker_id,size,dropped,picked,sender_id", 6)) {
            String parcelId = row[0];
            String lockerId = row[1];
            String size = row[2];
            if (!seenParcels.add(parcelId)) {
                throw new IllegalArgumentException("Duplicate parcel: " + parcelId);
            }
            if (!addresses.containsKey(lockerId)) {
                throw new IllegalArgumentException("Unknown locker: " + lockerId);
            }
            if (!Set.of("S", "M", "L").contains(size)) {
                throw new IllegalArgumentException("Invalid size: " + size);
            }

            Instant dropped = Instant.parse(row[3]);
            boolean picked = !row[4].isBlank();
            if (picked && Instant.parse(row[4]).isBefore(dropped)) {
                throw new IllegalArgumentException("Pickup before drop-off: " + parcelId);
            }
            LocalDate day = dropped.atOffset(ZoneOffset.UTC).toLocalDate();
            long[] count = groups.computeIfAbsent(
                    new GroupKey(lockerId, day, size), unused -> new long[2]);
            count[0]++;
            if (picked) count[1]++;
        }

        List<ReportRow> result = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            GroupKey key = entry.getKey();
            long[] count = entry.getValue();
            result.add(new ReportRow(key.lockerId, key.day, key.size,
                    addresses.get(key.lockerId), count[0], count[1]));
        }
        result.sort(Comparator.comparing(ReportRow::lockerId)
                .thenComparing(ReportRow::day).thenComparing(ReportRow::size));
        return result;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: ReferenceReport history.csv lockers.csv");
            System.exit(2);
        }
        System.out.println("locker_id,day,size,address,parcel_count,picked_count");
        for (ReportRow row : calculate(Path.of(args[0]), Path.of(args[1]))) {
            System.out.printf("%s,%s,%s,%s,%d,%d%n",
                    row.lockerId(), row.day(), row.size(), row.address(),
                    row.parcelCount(), row.pickedCount());
        }
    }
}
