-- Ресурс: комірки поштомата за розмірами
CREATE TABLE locker_cells (
                              locker_id TEXT    NOT NULL,
                              size      TEXT    NOT NULL CHECK (size IN ('S', 'M', 'L')),
    total     INTEGER NOT NULL CHECK (total >= 0),
    used      INTEGER NOT NULL CHECK (used >= 0),
    PRIMARY KEY (locker_id, size)
);

-- Журнал запитів на закладання посилок
CREATE TABLE drop_offs (
                           drop_id    TEXT PRIMARY KEY,
                           parcel_id  TEXT NOT NULL,
                           locker_id  TEXT NOT NULL,
                           size       TEXT NOT NULL CHECK (size IN ('S', 'M', 'L')),
    state      TEXT NOT NULL CHECK (state IN ('queued', 'processing', 'done', 'rejected', 'failed')),
    reason     TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_drop_offs_state ON drop_offs (state);

-- Демодані
INSERT INTO locker_cells (locker_id, size, total, used) VALUES
                                                            ('locker-001', 'S', 100, 0),
                                                            ('locker-001', 'M', 100, 0),
                                                            ('locker-001', 'L', 50,  0),
                                                            ('locker-002', 'S', 100, 0),
                                                            ('locker-002', 'M', 100, 0),
                                                            ('locker-002', 'L', 50,  0);