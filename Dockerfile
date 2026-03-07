# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system hostify && adduser --system --ingroup hostify hostify

COPY --from=builder /build/target/hostify-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/data && chown -R hostify:hostify /app
USER hostify

EXPOSE 8080

ENV DATABASE_PROFILE=h2 \
    ADMIN_USERNAME=admin \
    ADMIN_PASSWORD=admin

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
