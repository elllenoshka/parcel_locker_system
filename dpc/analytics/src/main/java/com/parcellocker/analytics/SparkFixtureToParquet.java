package com.parcellocker.analytics;

import org.apache.spark.sql.SparkSession;

/**
 * Reproducible small input for O4.2. Converts explicit CSV fixtures to Parquet.
 * CSV input columns are strings; timestamps use ISO 8601 (UTC in fixtures).
 */
public final class SparkFixtureToParquet {
    private SparkFixtureToParquet() {}

    public static void main(String[] args) {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: SparkFixtureToParquet history.csv lockers.csv history-out lockers-out");
        }
        SparkSession spark = SparkSession.builder()
                .appName("C11 Fixture to Parquet").getOrCreate();
        try {
            spark.read().option("header", "true").option("inferSchema", "false")
                    .option("nullValue", "").csv(args[0])
                    .write().mode("overwrite").parquet(args[2]);
            spark.read().option("header", "true").option("inferSchema", "false")
                    .csv(args[1]).write().mode("overwrite").parquet(args[3]);
        } finally {
            spark.stop();
        }
    }
}
