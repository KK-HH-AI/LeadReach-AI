# ── Build Stage ──────────────────────────────────────────────
FROM golang:1.25.0-alpine AS builder

WORKDIR /app

# 允许通过 --build-arg GOPROXY=xxx 指定 Go 代理（国内构建默认走 goproxy.cn）
ARG GOPROXY=https://goproxy.cn,direct
ENV GOPROXY=$GOPROXY

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build \
    -ldflags="-w -s" \
    -o /app/bin/gateway-service \
    ./cmd/main.go

# ── Runtime Stage ─────────────────────────────────────────────
# 用 alpine 而非 gcr.io/distroless：国内服务器拉取 gcr 不通，alpine 走 docker 加速器可拉
FROM alpine:3.20

WORKDIR /app

COPY --from=builder /app/bin/gateway-service /app/gateway-service
COPY --from=builder /app/configs /app/configs

EXPOSE 8080

ENTRYPOINT ["/app/gateway-service"]
