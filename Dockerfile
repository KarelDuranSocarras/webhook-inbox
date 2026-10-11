# ---- build stage ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

# ---- runtime stage (Debian/Ubuntu base: curl is available for the healthcheck) ----
FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --uid 10001 --create-home app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
USER app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]