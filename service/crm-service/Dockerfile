# crm-service 镜像（构建上下文是 service/，不是本目录）
# 构建：docker build -t leadreach/crm-service:latest -f crm-service/Dockerfile .
# ── Build Stage ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 先拷贝父/子 POM，利用 Docker 层缓存，避免每次重复下载依赖
COPY pom.xml ./
COPY crm-service/pom.xml ./crm-service/pom.xml
COPY leadreach-common/pom.xml ./leadreach-common/pom.xml

RUN mvn -pl crm-service dependency:go-offline -B -q || true

# 拷贝源码并打包（跳过测试）
COPY . .
RUN mvn -pl crm-service -am package -DskipTests -B -q

# ── Runtime Stage ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/crm-service/target/crm-service-*.jar /app/app.jar

EXPOSE 8083

# SPRING_PROFILES_ACTIVE 由 compose 传入，默认 prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]