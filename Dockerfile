# Etapa de compilación: Maven compila los módulos del backend con Java 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests package

# Render reutiliza la imagen y selecciona un JAR por servicio.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/msvc_*/target/*.jar /app/
COPY --from=build /workspace/msvc-eureka/target/*.jar /app/
COPY --from=build /workspace/msvc-gateway/target/*.jar /app/
COPY render-start.sh /app/render-start.sh
RUN chmod +x /app/render-start.sh
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "if [ \"$RENDER_AUTH_BUNDLE\" = \"true\" ] || { [ -z \"$RENDER_AUTH_BUNDLE\" ] && [ \"$SERVICE_JAR\" = \"msvc-gateway-0.0.1-SNAPSHOT.jar\" ]; }; then exec /app/render-start.sh; fi; test -n \"$SERVICE_JAR\" || { echo 'SERVICE_JAR must identify the executable Spring Boot JAR' >&2; exit 2; }; exec java -jar \"/app/$SERVICE_JAR\""]
