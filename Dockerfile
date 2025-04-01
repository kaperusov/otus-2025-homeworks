# =======================> build stage
FROM golang:1.24-alpine3.21 AS builder

WORKDIR /app

# download dependences
COPY go.mod go.sum ./
RUN go mod download

# build this
COPY main.go .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /app/server


# =======================> final stage
FROM alpine:3.21
ARG APP_PORT=8000
ENV APP_PORT=${APP_PORT}
ENV TZ="Europe/Moscow"
WORKDIR /opt

RUN apk add --no-cache tzdata

COPY --from=builder /app/server /opt/server

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# port and healthcheck
EXPOSE "${APP_PORT}"
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- "http://localhost:${APP_PORT}/health/" || exit 1

# ENTRYPOINT ["/opt/server", "--port", "8002"]
ENTRYPOINT /opt/server --port "${APP_PORT}"