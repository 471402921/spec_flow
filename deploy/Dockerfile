# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and POM files first (dependency caching layer)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY specflow-common/pom.xml specflow-common/pom.xml
COPY specflow-api/pom.xml specflow-api/pom.xml
COPY specflow-worker/pom.xml specflow-worker/pom.xml

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code (entire module dirs; pom.xml already cached above)
COPY specflow-common specflow-common
COPY specflow-api specflow-api
COPY specflow-worker specflow-worker

# Build the target module
ARG MODULE=specflow-api
RUN ./mvnw clean package -pl ${MODULE} -am -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre

WORKDIR /app

ARG MODULE=specflow-api
COPY --from=builder /build/${MODULE}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
