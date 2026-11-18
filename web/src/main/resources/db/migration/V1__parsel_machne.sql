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
	version bigint NOT NULL DEFAULT 1,
	FOREIGN KEY (locker_id) REFERENCES locker (id)
);

CREATE TABLE parcel(
	id text NOT NULL PRIMARY KEY,
	sender_id text NOT NULL, 
	recipient_phone text NOT NULL, 
	weight_g int NOT NULL, 
	size text NOT NULL CHECK (size IN ('S','M','L')),
	cell_id text NOT NULL, 
	status text NOT NULL CHECK (status IN ('held', 'dropped', 'picked', 'returned', 'expired', 'cancelled')),
	version bigint NOT NULL DEFAULT 1, 
	created_at timestamptz NOT NULL DEFAULT now(), 
	expires_at timestamptz NOT NULL,
	FOREIGN KEY (cell_id) REFERENCES cell(id)
);