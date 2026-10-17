-- =============================================================================
-- Konvensi umum semua tabel (Postgres only):
--   id         : bigint GENERATED ALWAYS AS IDENTITY (primary key)
--   uuid       : uuid v4 (default gen_random_uuid), di-lookup pakai hash index
--                (non-unique, sesuai keputusan: gak di-enforce unique)
--   audit      : created_at / created_by / updated_at / updated_by
--                deleted_at / deleted_by
--   *_by       : bigint, TANPA foreign key dan TANPA index (audit only)
--   index      : hanya kolom deleted_at yang di-index
--   unique     : selalu partial index WHERE deleted_at IS NULL (soft delete aware)
-- =============================================================================

CREATE TABLE pegawai (
    id         bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid       uuid         NOT NULL DEFAULT gen_random_uuid(),
    nama       varchar(255) NOT NULL,
    nip        varchar(255) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamptz,
    updated_by bigint,
    deleted_at timestamptz,
    deleted_by bigint
);

CREATE INDEX ix_pegawai_uuid_hash ON pegawai USING hash (uuid);
CREATE INDEX ix_pegawai_deleted_at ON pegawai (deleted_at);
CREATE UNIQUE INDEX ux_pegawai_nip ON pegawai (nip) WHERE deleted_at IS NULL;

CREATE TABLE users (
    id         bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid       uuid         NOT NULL DEFAULT gen_random_uuid(),
    username   varchar(255) NOT NULL,
    password   varchar(255) NOT NULL,
    pegawai_id bigint       NOT NULL REFERENCES pegawai (id),
    created_at timestamptz  NOT NULL DEFAULT now(),
    created_by bigint,
    updated_at timestamptz,
    updated_by bigint,
    deleted_at timestamptz,
    deleted_by bigint
);

CREATE INDEX ix_users_uuid_hash ON users USING hash (uuid);
CREATE INDEX ix_users_deleted_at ON users (deleted_at);
CREATE UNIQUE INDEX ux_users_username ON users (username) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_users_pegawai ON users (pegawai_id) WHERE deleted_at IS NULL;
