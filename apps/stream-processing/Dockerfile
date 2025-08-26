# Stage 1: Build stage with Maven
FROM maven:3.9.5-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .

COPY ./billing ./billing
COPY ./notification ./notification
COPY ./order ./order
COPY ./delivery ./delivery
COPY ./warehouse ./warehouse
RUN mvn dependency:go-offline
RUN mvn package -DskipTests

# Stage 2: Create billing application
FROM eclipse-temurin:21-jre-alpine AS billing-app

RUN addgroup -S javagroup && adduser -S javauser -G javagroup

WORKDIR /app
COPY --from=builder /app/billing/target/*.jar ./billing.jar
RUN chown -R javauser:javagroup /app

USER javauser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "billing.jar"]


# Stage 3: Create notification application
FROM eclipse-temurin:21-jre-alpine AS notification-app

RUN addgroup -S javagroup && adduser -S javauser -G javagroup

WORKDIR /app
COPY --from=builder /app/notification/target/*.jar ./notification.jar
RUN chown -R javauser:javagroup /app

USER javauser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "notification.jar"]

# Stage 4: Create order application
FROM eclipse-temurin:21-jre-alpine AS order-app

RUN addgroup -S javagroup && adduser -S javauser -G javagroup

WORKDIR /app
COPY --from=builder /app/order/target/*.jar ./order.jar
RUN chown -R javauser:javagroup /app

USER javauser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "order.jar"]


# Stage 5: Create warehouse application
FROM eclipse-temurin:21-jre-alpine AS warehouse-app

RUN addgroup -S javagroup && adduser -S javauser -G javagroup

WORKDIR /app
COPY --from=builder /app/warehouse/target/*.jar ./warehouse.jar
RUN chown -R javauser:javagroup /app

USER javauser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "warehouse.jar"]


# Stage 6: Create delivery application
FROM eclipse-temurin:21-jre-alpine AS delivery-app

RUN addgroup -S javagroup && adduser -S javauser -G javagroup

WORKDIR /app
COPY --from=builder /app/delivery/target/*.jar ./delivery.jar
RUN chown -R javauser:javagroup /app

USER javauser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "delivery.jar"]