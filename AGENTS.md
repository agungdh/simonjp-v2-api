# AGENTS.md

Panduan konvensi untuk SIMONJP. Baca ini sebelum menambah entity, migration, atau endpoint.
Dokumen ditulis dalam Bahasa Indonesia mengikuti gaya commit repo.

## Ringkasan stack

| Aspek | Nilai |
| --- | --- |
| Framework | Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7) |
| Java | 25 (toolchain), virtual threads aktif |
| Database | **PostgreSQL only** (18); `spring.jpa.hibernate.ddl-auto: validate` |
| Migration | Flyway — `src/main/resources/db/migration` |
| Cache/Session | Valkey/Redis via Spring Data Redis (`StringRedisTemplate`) |
| Object storage | MinIO (S3-compatible), belum ada client |
| Mapping | MapStruct 1.6.3 + Lombok |
| API docs | springdoc-openapi (`/swagger-ui.html`, `/v3/api-docs`) |
| JSON | Jackson 3 (`tools.jackson.*`) |

## Perintah penting

```bash
docker compose up -d          # postgres, valkey, minio, adminer (8083), redis-commander (8081)
./gradlew bootRun             # jalankan aplikasi (http://localhost:8080)
./gradlew test                # jalankan test (butuh postgres + valkey jalan)
./gradlew compileJava         # cek kompilasi cepat
```

Database dev: `localhost:5432/simonjp` (user/pass: `admin`/`admin`).
Valkey dev: `localhost:6379`.

## Struktur package

Base package: `id.my.agungdh.simonjp`

```
config/      konfigurasi Spring (@Configuration, properties binding)
domain/      JPA entity + BaseEntity
repository/  Spring Data JPA repository
security/    auth: filter, token service, principal, cookie/token helper
service/     logika bisnis, dibungkus @Transactional
controller/  REST controller + @RestControllerAdvice
dto/         request/response record
mapper/      MapStruct mapper
```

## Aturan skema database (WAJIB)

Semua tabel mengikuti konvensi berikut. **Postgres only** — jangan pakai fitur portabel lain bila
ada padanan Postgres yang lebih tepat.

1. **Primary key**
   `id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY`.
   Entity: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`.
   Karena `GENERATED ALWAYS`, insert dari SQL (seed) **tidak boleh** menyertakan kolom `id`
   (kalau terpaksa, pakai `OVERRIDING SYSTEM VALUE`).

2. **UUID**
   `uuid uuid NOT NULL DEFAULT gen_random_uuid()`.
   - Di-generate di Java (`UUID.randomUUID()` di `BaseEntity`) supaya nilainya langsung tersedia;
     `DEFAULT gen_random_uuid()` menutupi row yang di-insert lewat SQL.
   - Di-lookup pakai **hash index, non-unique**: `CREATE INDEX ix_<tabel>_uuid_hash ON <tabel> USING hash (uuid);`
   - **Tidak** di-enforce unique.

3. **Kolom audit** (semua ada di `BaseEntity`):
   `created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `deleted_by`.
   - `*_at` bertipe `timestamptz`. `created_at` `NOT NULL DEFAULT now()`.
   - `*_by` bertipe `bigint`, isinya id user. **Tanpa foreign key dan tanpa index** — murni audit.
   - Nilai `created_*`/`updated_*` diisi otomatis oleh JPA Auditing (`JpaAuditingConfig`).
   - Index **hanya** pada `deleted_at` (`ix_<tabel>_deleted_at`), karena query hampir selalu
     memfilter `deleted_at IS NULL`. `created_at`/`updated_at` **tidak** di-index.

4. **Soft delete**
   - Query baca otomatis terfilter: anotasi entity dengan `@SQLRestriction("deleted_at is null")`.
   - Soft delete lewat method repository (contoh `UserRepository.softDelete`) yang mengisi
     `deleted_at` + `deleted_by`. Jangan `delete()` hard delete entity yang mengikuti konvensi ini.

5. **Unique = partial index**
   Semua unique dibuat soft-delete aware:
   ```sql
   CREATE UNIQUE INDEX ux_<tabel>_<kolom> ON <tabel> (<kolom>) WHERE deleted_at IS NULL;
   ```

Contoh lengkap: `src/main/resources/db/migration/V1__init.sql`.

### Tabel saat ini
- `pegawai` — profil pegawai: `nama`, `nip` (unique partial).
- `users` — kredensial login: `username` (unique partial), `password` (BCrypt), `pegawai_id`
  (**relasi 1-1** ke `pegawai`, unique partial). FK ditaruh di `users` karena akun menempel ke
  data pegawai.

## Auth

- **Opaque token**, bukan JWT. Token acak 256-bit; yang disimpan di Valkey adalah
  **SHA-256 dari token** (key `auth:sess:<sha256>`), value `id|uuid|username|pegawaiId`.
- Dua cara kirim token:
  - **Web** → cookie HttpOnly (`app.auth.cookie-name`, default `simonjp_token`).
  - **Mobile/desktop** → header `Authorization: Bearer <token>` (juga dikembalikan di body login).
- **Sliding expiry**: tiap request terautentikasi, TTL di-reset ke penuh bila sisa TTL sudah
  melewati `app.auth.renew-threshold` (default `0.5`). Cookie ikut di-refresh saat renewal.
- Endpoint: `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`.
- Password di-hash dengan `BCryptPasswordEncoder`.
- CSRF dimatikan (API), cookie `HttpOnly` + `SameSite=Lax`. Aktifkan `cookie-secure` saat HTTPS.

Konfigurasi `app.auth.*` ada di `application.yaml`:
`token-ttl`, `renew-threshold`, `cookie-name`, `cookie-secure`, `cookie-same-site`.

Seed admin default: username/password `admin`/`admin` (`V2__seed_admin.sql`) — **wajib diganti**.

## Migration

- Nama file: `V<n>__<snake_case_deskripsi>.sql` di `src/main/resources/db/migration`.
- Migration **immutable** setelah di-commit; perubahan skema = file baru (`V3__...`).
- Jangan andalkan `ddl-auto` untuk mengubah skema (nilainya `validate`) — skema selalu lewat Flyway.
- Hash BCrypt untuk seed: generate mis. dengan `BCryptPasswordEncoder`, jangan tulis plaintext.

## Konvensi kode

- Lombok untuk boilerplate (`@Getter/@Setter/@NoArgsConstructor`). Hindari `@Data` pada entity.
- MapStruct (`componentModel = "spring"`) untuk mapping entity → DTO.
- DTO berupa `record` dan divalidasi dengan Jakarta Bean Validation (`@Valid` di controller).
- Service dibungkus `@Transactional`; akses relasi lazy dilakukan di dalam service.
- Error API dikembalikan lewat `ApiExceptionHandler` dengan bentuk `{ code, message }`.

## Golang/JS?

Bukan. Repo ini murni Java/Gradle. Jangan menambah tooling bahasa lain tanpa diminta.

