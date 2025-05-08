# =======================> build stage
FROM golang:1.24-alpine3.21 AS builder

WORKDIR /app

# download dependences
COPY go.mod go.sum ./
RUN go mod download

# build this
COPY ./internal ./internal
COPY ./docs     ./docs 
COPY main.go    .

RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /app/server


# =======================> final stage
FROM alpine:3.21
ENV TZ="Europe/Moscow"
WORKDIR /opt

RUN apk add --no-cache tzdata

COPY --from=builder /app/server /opt/server
COPY ./static /opt/static 

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# port and healthcheck
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- "http://localhost:8080/health/" || exit 1

CMD ["/opt/server"]