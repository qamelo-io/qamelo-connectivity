# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY qamelo-connectivity-common/pom.xml qamelo-connectivity-common/
COPY qamelo-connectivity-domain/pom.xml qamelo-connectivity-domain/
COPY qamelo-connectivity-infra/pom.xml qamelo-connectivity-infra/
COPY qamelo-connectivity-app/pom.xml qamelo-connectivity-app/
RUN mvn dependency:go-offline -B || true

COPY . .
RUN mvn clean package -DskipTests -B

# Stage 2: JVM Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/qamelo-connectivity-app/target/quarkus-app/ ./quarkus-app/

EXPOSE 9002
ENV JAVA_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENTRYPOINT ["java", "-jar", "quarkus-app/quarkus-run.jar"]
