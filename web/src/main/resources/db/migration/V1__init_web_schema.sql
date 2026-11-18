CREATE TABLE locker(
	id text PRIMARY KEY,
	address text NOT NULL,
	tz text NOT NULL
);

CREATE TABLE cell(
	id text PRIMARY KEY,
	locker_id text NOT NULL,
	size text NOT NULL CHECK (size IN ('S','M','L')),
	status text NOT NULL CHECK (status IN('free','occupied')),
	version bigint NOT NULL DEFAULT 1 CHECK(version > 0),
	FOREIGN KEY (locker_id) REFERENCES locker (id)
);

CREATE TABLE app_user (
    id text PRIMARY KEY,
    login text NOT NULL UNIQUE,
    name text NOT NULL,
    password_hash text NOT NULL,
    role text NOT NULL CHECK (role IN ('user', 'staff'))
);

CREATE TABLE parcel(
	id text PRIMARY KEY,
	sender_id text NOT NULL REFERENCES app_user(id),
    recipient_phone text NOT NULL,
	weight_g int NOT NULL CHECK ( weight_g > 0 ),
	size text NOT NULL CHECK (size IN ('S','M','L')),
	cell_id text NOT NULL REFERENCES cell(id),
	status text NOT NULL CHECK (status IN ('held', 'dropped', 'picked', 'returned', 'expired', 'cancelled')),
	version bigint NOT NULL DEFAULT 1, 
	created_at timestamptz NOT NULL DEFAULT now(), 
	expires_at timestamptz NOT NULL,
	CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX uq_parcel_active_cell
    ON parcel (cell_id)
    WHERE status IN ('held', 'dropped', 'returned');

CREATE INDEX idx_cell_available
    ON cell (locker_id, size)
    WHERE status = 'free';