package com.parcellocker.analytics;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Spark and independent Java reference on exactly the same 8 fixtures.
 * The Spark part is executed locally (two threads), not in the Docker cluster.
 */
class SparkBatchReportTest {

    @Test
    void sparkGroupsMatchReferenceExactly(@TempDir Path temp) throws Exception {
        SparkSession spark = SparkSession.builder()
                .master("local[2]")
                .appName("C11 LR4 parity test")
                .config("spark.ui.enabled", "false")
                .config("spark.sql.session.timeZone", "UTC")
                .config("spark.driver.host", "127.0.0.1")
                .getOrCreate();
        try {
            Path fixtures = Path.of("fixtures");
            var history = spark.read().option("header", "true")
                    .option("inferSchema", "false").option("nullValue", "")
                    .csv(fixtures.resolve("history.csv").toString());
            var lockers = spark.read().option("header", "true")
                    .option("inferSchema", "false")
                    .csv(fixtures.resolve("lockers.csv").toString());

            Path historyParquet = temp.resolve("history-parquet");
            Path lockersParquet = temp.resolve("lockers-parquet");
            history.write().parquet(historyParquet.toString());
            lockers.write().parquet(lockersParquet.toString());
            var actual = SparkBatchReport.build(
                    spark.read().parquet(historyParquet.toString()),
                    spark.read().parquet(lockersParquet.toString()))
                    // Convert the SQL DATE to its ISO string for Java-side test assertions.
                    // The actual Spark/Parquet report keeps the DATE type unchanged.
                    .withColumn("day", org.apache.spark.sql.functions.col("day").cast("string"))
                    .collectAsList();

            Map<String, ReferenceReport.ReportRow> expected = ReferenceReport.calculate(
                    fixtures.resolve("history.csv"), fixtures.resolve("lockers.csv"))
                    .stream().collect(Collectors.toMap(
                            row -> row.lockerId() + ":" + row.day() + ":" + row.size(),
                            Function.identity()));

            assertEquals(expected.size(), actual.size());
            for (var row : actual) {
                String key = row.getAs("locker_id") + ":" + row.getAs("day")
                        + ":" + row.getAs("size");
                ReferenceReport.ReportRow reference = expected.get(key);
                // String-form date ensures compatibility with Spark's java.sql.Date.
                assertEquals(reference.day(), LocalDate.parse(row.getAs("day").toString()));
                assertEquals(reference.address(), row.getAs("address"));
                assertEquals(reference.parcelCount(), ((Number) row.getAs("parcel_count")).longValue());
                assertEquals(reference.pickedCount(), ((Number) row.getAs("picked_count")).longValue());
            }
        } finally {
            spark.stop();
        }
    }
}
