package com.parcellocker.dpc.repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.parcellocker.dpc.api.DropOffRequest;
import com.parcellocker.dpc.model.DropOff;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;

@Repository
public class DropOffRepository {

    private static final RowMapper<DropOff> MAPPER = (rs, rowNum) -> new DropOff(
            rs.getString("drop_id"),
            rs.getString("parcel_id"),
            rs.getString("locker_id"),
            rs.getString("size"),
            rs.getString("state"),
            rs.getString("reason"),
            utc(rs.getObject("created_at", OffsetDateTime.class)),
            utc(rs.getObject("updated_at", OffsetDateTime.class)));

    private final JdbcClient jdbc;

    public DropOffRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Створює запис зі станом queued. Повертає false, якщо такий drop_id уже є. */
    public boolean insertQueued(DropOffRequest request) {
        int rows = jdbc.sql("""
                INSERT INTO drop_offs (drop_id, parcel_id, locker_id, size, state)
                VALUES (:dropId, :parcelId, :lockerId, :size, 'queued')
                ON CONFLICT (drop_id) DO NOTHING
                """)
                .param("dropId", request.dropId())
                .param("parcelId", request.parcelId())
                .param("lockerId", request.lockerId())
                .param("size", request.size())
                .update();
        return rows == 1;
    }

    public Optional<DropOff> findById(String dropId) {
        return jdbc.sql("SELECT * FROM drop_offs WHERE drop_id = :dropId")
                .param("dropId", dropId)
                .query(MAPPER)
                .optional();
    }

    private static OffsetDateTime utc(OffsetDateTime time) {
        return time.withOffsetSameInstant(ZoneOffset.UTC);
    }
    /** Змінює стан запиту (queued -> processing -> done | rejected | failed). */
    public void updateState(String dropId, String state, String reason) {
        jdbc.sql("""
                UPDATE drop_offs
                SET state = :state, reason = :reason, updated_at = now()
                WHERE drop_id = :dropId
                """)
                .param("state", state)
                .param("reason", reason, Types.VARCHAR)
                .param("dropId", dropId)
                .update();
    }

    public void delete(String dropId) {
        jdbc.sql("DELETE FROM drop_offs WHERE drop_id = :dropId")
                .param("dropId", dropId)
                .update();
    }
}