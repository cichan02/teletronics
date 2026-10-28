# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-jammy AS deps
WORKDIR /build
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -P release -D skipTests

FROM deps AS package
WORKDIR /build
COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -P release && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

FROM package AS extract
WORKDIR /build
RUN java -Djarmode=tools -jar target/app.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:25-jdk-alpine-3.24 AS jre
RUN jlink \
    --add-modules java.base,java.desktop,java.logging,java.naming,java.management,java.instrument,java.security.jgss,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.management,java.xml,java.sql,java.net.http \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=zip-6 \
    --output /opt/jre

FROM alpine:3.24 AS prod
ARG UID=10001
RUN apk add --no-cache ca-certificates && \
    addgroup -g "${UID}" appuser && \
    adduser -D -H -u "${UID}" -G appuser -s /sbin/nologin appuser
ENV JAVA_HOME=/opt/jre
ENV PATH="/opt/jre/bin:${PATH}"
WORKDIR /app
RUN mkdir -p uploads && chown appuser:appuser uploads
COPY --from=jre /opt/jre /opt/jre
COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./
USER appuser
VOLUME /app/uploads
EXPOSE 56789
ENTRYPOINT [ "java", "-Dspring.profiles.active=release", "org.springframework.boot.loader.launch.JarLauncher" ]
