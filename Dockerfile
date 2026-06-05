# =========================
# Build Stage
# =========================
FROM maven:3.9-eclipse-temurin-22 AS build

WORKDIR /workspace

# copy POM and Maven wrapper
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw
# Pre-load all dependencies
RUN ./mvnw dependency:go-offline

COPY src src

# build application
RUN ./mvnw clean package -DskipTests

# extract Spring Boot layers
RUN java -Djarmode=layertools \
    -jar target/*.jar extract


# =========================
# Runtime Stage
# =========================
FROM eclipse-temurin:22-jre

# create non-root user
RUN groupadd --system app_group && \
    useradd --system --gid app_group app_user

WORKDIR /app

# copy each layer separately to optimize cache usage
COPY --from=build /workspace/dependencies/ ./
COPY --from=build /workspace/spring-boot-loader/ ./
COPY --from=build /workspace/snapshot-dependencies/ ./
COPY --from=build /workspace/application/ ./

# change the owner of the application files to app_user
RUN chown -R app_user:app_group /app

# use app_user in container
USER app_user

EXPOSE 8080

# start JarLauncher directly since no JAR file exists
ENTRYPOINT ["java", \
            "-XX:InitialRAMPercentage=25.0", \
            "-XX:MaxRAMPercentage=75.0", \
            "org.springframework.boot.loader.launch.JarLauncher"]