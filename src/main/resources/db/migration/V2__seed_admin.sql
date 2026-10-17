-- Seed admin default (username/password: admin/admin). WAJIB diganti setelah login pertama.
-- CATATAN: id memakai GENERATED ALWAYS, jadi kolom id tidak ikut di-insert.

INSERT INTO pegawai (nama, nip)
VALUES ('Administrator', 'ADMIN-001');

INSERT INTO users (username, password, pegawai_id)
SELECT 'admin', '$2a$10$o9yLAPQGE9d1rkZBx7/4kun38wr3reQs6sana9NPxKnOMd3EcdU.q', p.id
FROM pegawai p
WHERE p.nip = 'ADMIN-001';
