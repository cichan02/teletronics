# 📁 Teletronics

**Teletronics** is a lightweight backend service that provides a file storage and sharing platform through a clean RESTful API.

It allows users to **upload**, **organize**, **share** and **manage** files — with automatic content type detection, deduplication, tag-based listing, sorting and pagination, and expiring download links.

---

## ✨ Features

- 📤 **Upload files** via `multipart/form-data` with optional metadata part
- 🔍 **Automatic content type detection** via Apache Tika (with user-supplied override validation)
- 🛡 **Duplicate protection** — rejects uploads with the same content hash or filename (per user)
- 📚 **Listing** — public / personal file lists with tag filtering, sorting and pagination
- 🔄 **Renaming** — pure metadata/path update, no re-upload required
- 🔗 **Sharing** — HMAC-SHA256 signed, expiring download tokens
- 🏷 **Tags** — up to 5 tags per file

---

## 🚀 Getting Started

### ⚙️ Environment Variables

| Variable | Description |
|---|---|
| `MONGO_HOST` | MongoDB host |
| `MONGO_PORT` | MongoDB port |
| `MONGO_DB` | MongoDB database name |
| `DOWNLOAD_TOKEN_SECRET` | Secret used for HMAC-SHA256 download token signing |

---

### 🐳 Run with Docker (recommended)

The project ships with a `docker-compose.yml` that runs the app together with MongoDB.

```yaml
services:
  app:
    image: cichan/teletronics:latest
    container_name: teletronics
    restart: unless-stopped
    environment:
      MONGO_HOST: your-mongno-host
      MONGO_PORT: your-mongno-port
      MONGO_DB: your-mongno-database-name
      DOWNLOAD_TOKEN_SECRET: Generate cryptographically secure secrets length = 32 bytes (256 bits)
    ports:
      - "56789:56789"
    mem_limit: 1g
    volumes:
      - uploads:/app/uploads
    depends_on:
      mongodb:
        condition: service_healthy

  mongodb:
    image: mongo:9.0.4
    container_name: mongodb
    restart: unless-stopped
    ports:
      - "27017:27017"
    volumes:
      - mongodb-config:/data/configdb
      - mongodb-data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping').ok"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  uploads:
  mongodb-config:
  mongodb-data:
```

Start everything:

```bash
docker compose up -d
```

The application will be available at: <http://localhost:56789/api/v1/teletronics>

---

### 🏃 Run Locally (without Docker)

Example (run the app against a local MongoDB instance):

```bash
./mvnw clean package -P release && java -jar target/*.jar
```

---

## 📡 API Usage

### Conventions

- User context is passed via the `X-User-Id` header (UUID) — no session or user management endpoints.
- List endpoints accept query parameters: `tag`, `pageNumber`, `size`, `sortBy`, `direction`.
- JSON request bodies use `Content-Type: application/json`.

---

### 1. Upload a file

```bash
curl -X POST http://localhost:56789/api/v1/teletronics/files/upload \
  -H "X-User-Id: 9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660" \
  -F 'file=@/path/to/yourfile.txt' \
  -F 'uploadRequest=@/path/to/upload-request.json;type=application/json'
```

`upload-request.json`:

```json
{
  "filename": "my-file.txt",
  "contentType": "text/plain",
  "visibility": "PUBLIC",
  "tags": ["java", "spring"]
}
```

**Response** `200 OK`:

```json
{
  "id": "9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660",
  "filename": "my-file.txt",
  "visibility": "PUBLIC",
  "contentType": "text/plain",
  "fileSize": 12,
  "tags": ["java", "spring"],
  "uploadDate": "2026-10-28T18:00:00Z"
}
```

---

### 2. List public files

```bash
curl -X GET "http://localhost:56789/api/v1/teletronics/files/public?tag=java&pageNumber=0&size=20&sortBy=UPLOAD_DATE&direction=DESC"
```

---

### 3. List my files

```bash
curl -X GET "http://localhost:56789/api/v1/teletronics/files/my?tag=java&pageNumber=0&size=20&sortBy=FILENAME&direction=ASC" \
  -H "X-User-Id: 9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660"
```

---

### 4. Rename a file

```bash
curl -X PATCH http://localhost:56789/api/v1/teletronics/files/9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660/filename \
  -H "X-User-Id: 9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660" \
  -H "Content-Type: application/json" \
  -d '{"filename": "new-name.txt"}'
```

---

### 5. Delete a file

```bash
curl -X DELETE http://localhost:56789/api/v1/teletronics/files/9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660 \
  -H "X-User-Id: 9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660"
```

---

### 6. Generate download link

```bash
curl -X GET http://localhost:56789/api/v1/teletronics/generate-link/9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660 \
  -H "X-User-Id: 9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660"
```

**Response**:

```json
{
  "link": "http://localhost:56789/api/v1/teletronics/download?token=...",
  "expiresAt": "2026-10-28T18:15:00Z"
}
```

---

### 7. Download a file

```bash
curl -X GET "http://localhost:56789/api/v1/teletronics/download?token={token}"
```

The response is the file content with `Content-Disposition: attachment` and the detected MIME type.

---

### 8. List tags

```bash
curl -X GET http://localhost:56789/api/v1/teletronics/tags/public
curl -X GET http://localhost:56789/api/v1/teletronics/tags/my \
  -H "X-User-Id: 9c4a842e-1c16-4a55-ae0b-ccbfe7a3a660"
```

---
