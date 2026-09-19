# ── Build Stage ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 先拷贝父/子 POM，利用 Docker 层缓存，避免每次重复下载依赖
COPY pom.xml ./
COPY user-service/pom.xml ./user-service/
COPY leadreach-common/pom.xml ./leadreach-common/pom.xml

RUN mvn -pl user-service dependency:go-offline -B -q || true

# 拷贝源码并打包（跳过测试）
COPY . .
RUN mvn -pl user-service -am package -DskipTests -B -q

# ── Runtime Stage ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/user-service/target/user-service-*.jar /app/app.jar

EXPOSE 8081

# SPRING_PROFILES_ACTIVE 由 compose 传入，默认 prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]