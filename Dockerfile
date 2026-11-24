# Etapa de compilación: Maven compila los módulos del backend con Java 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests package

# Render puede reutilizar esta imagen para cada servicio seleccionando SERVICE_JAR.
# Si el servicio se crea manualmente sin esa variable, inicia el Gateway por defecto.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/msvc_*/target/*.jar /app/
ENV SERVICE_JAR=msvc-gateway-0.0.1-SNAPSHOT.jar
ENV EUREKA_CLIENT_ENABLED=false
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=40 -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=32m -XX:+UseSerialGC"
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "SERVICE_JAR=\"${SERVICE_JAR:-msvc-gateway-0.0.1-SNAPSHOT.jar}\"; if [ ! -f \"/app/$SERVICE_JAR\" ]; then echo \"SERVICE_JAR not found: /app/$SERVICE_JAR. Check its exact value against DEPLOYMENT.md.\" >&2; exit 2; fi; exec java -jar \"/app/$SERVICE_JAR\""]
COPY --from=build /workspace/msvc-eureka/target/*.jar /app/
COPY --from=build /workspace/msvc-gateway/target/*.jar /app/
