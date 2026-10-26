# Teletronics File Storage Service

A RESTful file storage service built with Spring Boot and MongoDB that allows users to upload, manage, and share files with fine-grained access control.

[![CI/CD Pipeline](https://github.com/YOUR_USERNAME/teletronics/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/teletronics/actions)

## Features

### Functional Requirements

- **File Upload**
  - Upload files with custom filenames
  - Set visibility (PUBLIC/PRIVATE)
  - Tag files (up to 5 tags per file)
  - No file size limits (supports files from KB to hundreds of GB)
  - Automatic content type detection
  - Unique, non-guessable download links

- **Duplicate Prevention**
  - Files are considered duplicates if:
    - Content is identical (SHA-256 hash)
    - OR filename already exists for the user
  - Duplicates are rejected at upload time

- **File Management**
  - Rename uploaded files
  - Delete own files only
  - List all PUBLIC files
  - List files owned by the user

- **Advanced Filtering & Pagination**
  - Filter by tags (case-insensitive)
  - Sort by: filename, upload date, content type, file size
  - Configurable page size and pagination
  - Sort direction (ascending/descending)

- **Secure Downloads**
  - Download files via unique token
  - Both PUBLIC and PRIVATE files downloadable by token
  - No link guessing (UUID-based tokens)

- **Tag Management**
  - List all available tags
  - Case-insensitive tag matching

### Non-Functional Requirements

- **Resource Limits**
  - Container memory limit: 1GB
  - Application disk limit: 200MB (configurable)
  - JVM configured for optimal memory usage

- **Technology Stack**
  - Spring Boot 3.5.16
  - MongoDB 7.0
  - Java 21
  - Maven build system

- **Testing**
  - Comprehensive unit tests (23 tests)
  - Integration tests (22 tests)
  - 100% coverage of core functionality

- **Containerization**
  - Docker image with resource constraints
  - Multi-stage build for optimized image size
  - Health checks included

- **CI/CD**
  - GitHub Actions workflow
  - Automated testing on push/PR
  - Docker image build and push
  - Security scanning with Trivy

## API Documentation

### Authentication

All endpoints (except download and public listing) require a user identifier via the `X-User-Id` header.

```bash
X-User-Id: john.doe
```

### Endpoints

#### 1. Upload File

Upload a file with metadata.

```http
POST /api/files/upload
Content-Type: multipart/form-data
X-User-Id: john.doe

Parameters:
- file: The file to upload (required)
- filename: Custom filename (optional, uses original if not provided)
- visibility: PUBLIC or PRIVATE (required)
- tags: Array of tags, max 5 (optional)
- contentType: MIME type (optional, auto-detected if not provided)
```

**Example using cURL:**

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "X-User-Id: john.doe" \
  -F "file=@/path/to/document.pdf" \
  -F "filename=important-doc.pdf" \
  -F "visibility=PRIVATE" \
  -F "tags=work" \
  -F "tags=documentation"
```

**Response:**

```json
{
  "fileId": "65a1b2c3d4e5f6a7b8c9d0e1",
  "filename": "important-doc.pdf",
  "contentType": "application/pdf",
  "fileSize": 1048576,
  "downloadLink": "http://localhost:8080/api/files/download/a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6",
  "uploadDate": "2026-10-26T10:30:00Z"
}
```

#### 2. Update Filename

Change the filename of an uploaded file.

```http
PUT /api/files/{fileId}/filename
Content-Type: application/json
X-User-Id: john.doe

{
  "filename": "new-filename.pdf"
}
```

**Example:**

```bash
curl -X PUT http://localhost:8080/api/files/65a1b2c3d4e5f6a7b8c9d0e1/filename \
  -H "X-User-Id: john.doe" \
  -H "Content-Type: application/json" \
  -d '{"filename":"renamed-document.pdf"}'
```

#### 3. List Public Files

Get a paginated list of all public files.

```http
GET /api/files/public?tag={tag}&page={page}&size={size}&sortBy={field}&sortDirection={asc|desc}
```

**Parameters:**
- `tag` (optional): Filter by tag (case-insensitive)
- `page` (optional, default: 0): Page number
- `size` (optional, default: 20): Items per page
- `sortBy` (optional, default: uploadDate): Sort field (filename, uploadDate, contentType, fileSize)
- `sortDirection` (optional, default: desc): Sort direction (asc, desc)

**Example:**

```bash
curl "http://localhost:8080/api/files/public?tag=documentation&page=0&size=10&sortBy=uploadDate&sortDirection=desc"
```

**Response:**

```json
{
  "content": [
    {
      "id": "65a1b2c3d4e5f6a7b8c9d0e1",
      "filename": "document.pdf",
      "fileSize": 1048576,
      "contentType": "application/pdf",
      "visibility": "PUBLIC",
      "uploadDate": "2026-10-26T10:30:00Z",
      "tags": ["documentation", "work"],
      "downloadLink": "http://localhost:8080/api/files/download/a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### 4. List User's Files

Get a paginated list of files uploaded by the authenticated user.

```http
GET /api/files/my-files?tag={tag}&page={page}&size={size}&sortBy={field}&sortDirection={asc|desc}
X-User-Id: john.doe
```

**Example:**

```bash
curl -H "X-User-Id: john.doe" \
  "http://localhost:8080/api/files/my-files?page=0&size=20"
```

#### 5. Delete File

Delete a file (only files owned by the user can be deleted).

```http
DELETE /api/files/{fileId}
X-User-Id: john.doe
```

**Example:**

```bash
curl -X DELETE http://localhost:8080/api/files/65a1b2c3d4e5f6a7b8c9d0e1 \
  -H "X-User-Id: john.doe"
```

#### 6. Download File

Download a file using its unique download token.

```http
GET /api/files/download/{token}
```

**Example:**

```bash
curl -O -J http://localhost:8080/api/files/download/a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6
```

#### 7. List All Tags

Get a list of all available tags in the system.

```http
GET /api/tags
```

**Example:**

```bash
curl http://localhost:8080/api/tags
```

**Response:**

```json
[
  {
    "id": "65a1b2c3d4e5f6a7b8c9d0e2",
    "name": "documentation"
  },
  {
    "id": "65a1b2c3d4e5f6a7b8c9d0e3",
    "name": "work"
  }
]
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.9+
- MongoDB 7.0+ (or use Docker Compose)
- Docker and Docker Compose (for containerized deployment)

### Running Locally

#### Option 1: Using Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/teletronics.git
cd teletronics

# Create uploads directory
mkdir uploads

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

The application will be available at `http://localhost:8080`

#### Option 2: Manual Setup

```bash
# Start MongoDB
docker run -d -p 27017:27017 --name mongodb mongo:7.0

# Build and run the application
./mvnw clean package
java -jar target/teletronics-1.0.0.jar

# Or use Maven Spring Boot plugin
./mvnw spring-boot:run
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run tests with coverage
./mvnw test jacoco:report

# Run specific test class
./mvnw test -Dtest=FileStorageServiceTest
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t teletronics:latest .
```

### Run Docker Container

```bash
# Run MongoDB
docker run -d --name mongodb \
  -p 27017:27017 \
  mongo:7.0

# Run application
docker run -d --name teletronics-app \
  -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/teletronics \
  -m 1g \
  --memory-reservation 512m \
  teletronics:latest
```

### Resource Limits

The Docker container is configured with:
- **Memory limit**: 1GB
- **Memory reservation**: 512MB
- **JVM heap**: 768MB max (-Xmx768m)
- **Application disk**: 200MB (configurable via volume)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATA_MONGODB_URI` | MongoDB connection string | `mongodb://localhost:27017/teletronics` |
| `FILE_STORAGE_LOCATION` | Directory for file storage | `./uploads` |
| `SERVER_PORT` | Application port | `8080` |
| `JAVA_OPTS` | JVM options | `-Xmx768m -Xms256m` |

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/teletronics

# File Upload
spring.servlet.multipart.max-file-size=-1
spring.servlet.multipart.max-request-size=-1
file.storage.location=./uploads

# Server
server.port=8080
```

## Architecture

### Project Structure

```
teletronics/
├── src/
│   ├── main/
│   │   ├── java/by/piskunou/teletronics/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # MongoDB entities
│   │   │   ├── repository/      # MongoDB repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── TeletronicsApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/by/piskunou/teletronics/
│       │   ├── controller/      # Integration tests
│       │   └── service/         # Unit tests
│       └── resources/
│           └── application-test.yml
├── .github/
│   └── workflows/
│       └── ci.yml               # CI/CD pipeline
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

### Data Model

#### User
- `id`: String (MongoDB ObjectId)
- `username`: String (unique, indexed)
- `password`: String
- `createdAt`: Instant

#### FileMetadata
- `id`: String (MongoDB ObjectId)
- `filename`: String
- `originalFilename`: String
- `storagePath`: String
- `fileSize`: Long
- `contentType`: String
- `downloadToken`: String (UUID, unique, indexed)
- `fileHash`: String (SHA-256)
- `visibility`: Enum (PUBLIC, PRIVATE)
- `uploadDate`: Instant (indexed)
- `userId`: String (indexed)
- `tags`: Set<String>

#### Tag
- `id`: String (MongoDB ObjectId)
- `name`: String
- `nameLower`: String (unique, indexed, for case-insensitive matching)

## CI/CD Pipeline

The project uses GitHub Actions for continuous integration and deployment:

1. **Test**: Runs all unit and integration tests
2. **Build**: Builds Docker image and pushes to Docker Hub
3. **Security Scan**: Scans image for vulnerabilities using Trivy

### Setup GitHub Actions

1. Add secrets to your GitHub repository:
   - `DOCKER_USERNAME`: Your Docker Hub username
   - `DOCKER_PASSWORD`: Your Docker Hub password/token

2. Push to `master` or `main` branch to trigger the pipeline

## API Documentation (Swagger)

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

## Health Checks

The application exposes health endpoints:

- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

## Troubleshooting

### Common Issues

**MongoDB Connection Failed**
```bash
# Check MongoDB is running
docker ps | grep mongo

# Check connection string
echo $SPRING_DATA_MONGODB_URI
```

**Out of Memory**
```bash
# Increase container memory limit
docker run -m 2g ...

# Or adjust JVM options
export JAVA_OPTS="-Xmx1024m -Xms512m"
```

**File Upload Failed**
```bash
# Check uploads directory exists and has write permissions
mkdir -p ./uploads
chmod 755 ./uploads
```

## Performance Considerations

- **File Storage**: Files are stored on disk, not in MongoDB
- **Indexing**: MongoDB indexes on userId, visibility, uploadDate, and downloadToken
- **Pagination**: All list endpoints support pagination to handle large datasets
- **Content Type Detection**: Uses Apache Tika for accurate MIME type detection

## Security

- **Authentication**: Simple header-based authentication (X-User-Id)
- **Authorization**: Users can only delete/modify their own files
- **Download Tokens**: UUID-based tokens prevent link guessing
- **File Isolation**: Each user's files are isolated by userId
- **Container Security**: Non-root user, minimal base image

⚠️ **Note**: This is a demo application. For production use, implement proper authentication (e.g., JWT, OAuth2) and add HTTPS support.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is created for demonstration purposes.

## Contact

Project Link: [https://github.com/YOUR_USERNAME/teletronics](https://github.com/YOUR_USERNAME/teletronics)
