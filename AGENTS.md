# AGENTS.md

Aplikasi Spring Boot untuk SIMONJP. Saat ini isinya fondasi auth/login (`users` + `pegawai`).
Dokumen ditulis Bahasa Indonesia mengikuti gaya commit repo.

## Stack & quirk penting

| Aspek | Nilai |
| --- | --- |
| Framework | Spring Boot 4.1.1 / Spring Framework 7 / Spring Security 7 |
| Java | **25** lewat Gradle toolchain — jangan andalkan `java` di PATH (bisa versi lain) |
| DB | **PostgreSQL only** (18), `ddl-auto: validate`, `open-in-view: false` |
| Migration | Flyway (`src/main/resources/db/migration`) |
| Cache/session | Valkey/Redis (`StringRedisTemplate`) |
| Mapping | MapStruct 1.6.3 + Lombok (binding sudah di-configure) |
| JSON | **Jackson 3** → import `tools.jackson.*`, **bukan** `com.fasterxml.jackson.*` |
| API docs | springdoc (`/swagger-ui.html`, `/v3/api-docs`) |

Quirk yang gampang kelewat:

- `ddl-auto: validate` → skema **hanya** dari Flyway. Ubah entity tanpa migration yang cocok =
  aplikasi gagal start.
- `open-in-view: false` → akses relasi lazy **harus** di dalam `@Transactional` (di service).
- Test pakai DB dev yang **sama** (`simonjp`) — tidak ada `src/test/resources`, jadi `./gradlew test`
  menjalankan Flyway ke DB dev dan butuh Postgres + Valkey hidup.

## Perintah

```bash
docker compose up -d     # prasyarat run & test: postgres + valkey (plus minio, adminer:8083, redis-commander:8081)
./gradlew bootRun        # app di http://localhost:8080
./gradlew test           # butuh postgres + valkey jalan (tidak ada Testcontainers)
./gradlew test --tests 'id.my.agungdh.simonjp.SimonjpApplicationTests'   # satu test
./gradlew compileJava    # cek kompilasi cepat
```

DB dev `localhost:5432/simonjp` (`admin`/`admin`); Valkey `localhost:6379`.
Base package `id.my.agungdh.simonjp`; struktur: `config/ domain/ repository/ security/ service/ controller/ dto/ mapper/`.

## Konvensi skema (WAJIB, semua tabel baru)

Postgres only. Contoh lengkap: `db/migration/V1__init.sql`.

- **PK** `id bigint GENERATED ALWAYS AS IDENTITY` + `@GeneratedValue(strategy = IDENTITY)`.
  Seed SQL **tidak boleh** menyertakan kolom `id` (kecuali `OVERRIDING SYSTEM VALUE`).
- **uuid** `uuid NOT NULL DEFAULT gen_random_uuid()`, digenerate di Java (`BaseEntity`), di-lookup
  pakai **hash index non-unique**: `CREATE INDEX ix_<tabel>_uuid_hash ON <tabel> USING hash (uuid);`.
  Tidak di-enforce unique.
- **Audit** (semua di `BaseEntity`): `created_at/by`, `updated_at/by`, `deleted_at/by`.
  - `*_at` = `timestamptz` (`created_at NOT NULL DEFAULT now()`); `*_by` = `bigint` id user.
  - `*_by` **tanpa FK dan tanpa index**. Index **hanya** pada `deleted_at` (`ix_<tabel>_deleted_at`).
  - `created_*`/`updated_*` diisi JPA Auditing (`JpaAuditingConfig`).
- **Soft delete**: entity diberi `@SQLRestriction("deleted_at is null")`; hapus via method repository
  yang mengisi `deleted_at`+`deleted_by` (mis. `UserRepository.softDelete`), bukan `delete()`.
- **Unique selalu partial** (soft-delete aware):
  `CREATE UNIQUE INDEX ux_<tabel>_<kolom> ON <tabel> (<kolom>) WHERE deleted_at IS NULL;`

Tabel saat ini: `pegawai` (`nama`, `nip` unique partial); `users` (`username` unique partial,
`password` BCrypt, `pegawai_id` = relasi **1-1** ke `pegawai` dengan FK ditaruh di `users`).

## Auth (opaque token di Valkey)

- **Bukan JWT**. Token acak 256-bit; yang disimpan di Valkey = **hash SHA-256 token**, key
  `auth:sess:<sha256>` (raw token tidak pernah persist), **value = `userId` saja**. Data lain
  (`uuid`, `username`, `pegawaiId`) diambil dari DB tiap request (`AuthenticatedUserService`),
  jadi session tidak basi dan user soft-deleted otomatis ditolak (session + cookie dibuang).
- Kirim via cookie HttpOnly (web; `app.auth.cookie-name`) **atau** `Authorization: Bearer`
  (mobile/desktop; token juga dikembalikan di body login).
- **Sliding expiry**: TTL di-reset ke penuh bila sisa TTL < `app.auth.renew-threshold` × TTL;
  cookie ikut di-refresh saat renewal.
- Endpoint: `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`.
- `SecurityConfig`: stateless, CSRF off, cookie `HttpOnly`+`SameSite=Lax`. Set
  `app.auth.cookie-secure: true` kalau sudah HTTPS.
- Seed default `admin`/`admin` (`V2__seed_admin.sql`) — wajib diganti.

## Migration & kode

- File `V<n>__<snake_case>.sql`; **immutable** setelah di-commit → perubahan skema = file baru.
- Hash BCrypt untuk seed: hasil `BCryptPasswordEncoder`, jangan plaintext.
- MapStruct wajib `@Mapper(componentModel = "spring")`. DTO = `record` + Bean Validation (`@Valid`).
- Error API seragam lewat `ApiExceptionHandler` (`{ code, message }`).
