package com.parcellocker.dpc.repository;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class LockerCellRepository {

    /** Рядок ресурсу: done — скільки закладань у журналі мають стан done. */
    public record CellRow(String size, int total, int used, int done) {
    }

    private final JdbcClient jdbc;

    public LockerCellRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(String lockerId, String size) {
        Long count = jdbc.sql("""
                SELECT count(*) FROM locker_cells
                WHERE locker_id = :lockerId AND size = :size
                """)
                .param("lockerId", lockerId)
                .param("size", size)
                .query(Long.class)
                .single();
        return count > 0;
    }

    public List<CellRow> findByLocker(String lockerId) {
        return jdbc.sql("""
                SELECT c.size, c.total, c.used,
                       (SELECT count(*) FROM drop_offs d
                         WHERE d.locker_id = c.locker_id
                           AND d.size = c.size
                           AND d.state = 'done') AS done_count
                FROM locker_cells c
                WHERE c.locker_id = :lockerId
                ORDER BY CASE c.size WHEN 'S' THEN 1 WHEN 'M' THEN 2 ELSE 3 END
                """)
                .param("lockerId", lockerId)
                .query((rs, rowNum) -> new CellRow(
                        rs.getString("size"),
                        rs.getInt("total"),
                        rs.getInt("used"),
                        rs.getInt("done_count")))
                .list();
    }
    /** Лічильник комірок одного розміру в одному поштоматі. */
    public record Counter(int total, int used) {
    }

    public Optional<Counter> findCounter(String lockerId, String size) {
        return jdbc.sql("""
                SELECT total, used FROM locker_cells
                WHERE locker_id = :lockerId AND size = :size
                """)
                .param("lockerId", lockerId)
                .param("size", size)
                .query((rs, rowNum) -> new Counter(rs.getInt("total"), rs.getInt("used")))
                .optional();
    }

    public void setUsed(String lockerId, String size, int used) {
        jdbc.sql("""
                UPDATE locker_cells SET used = :used
                WHERE locker_id = :lockerId AND size = :size
                """)
                .param("used", used)
                .param("lockerId", lockerId)
                .param("size", size)
                .update();
    }
}