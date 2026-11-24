#!/bin/sh
set -eu

APP_DIR="/app"
EUREKA_URL="${EUREKA_URL:-http://127.0.0.1:8761/eureka/}"
GATEWAY_PORT="${PORT:-10000}"
JAVA_XMX="${JAVA_XMX:-128m}"
JAVA_MAX_METASPACE="${JAVA_MAX_METASPACE:-128m}"
JAVA_CODE_CACHE="${JAVA_CODE_CACHE:-32m}"
EUREKA_JAR="$APP_DIR/msvc-eureka-0.0.1-SNAPSHOT.jar"
GATEWAY_JAR="$APP_DIR/msvc-gateway-0.0.1-SNAPSHOT.jar"
SERVICE_PIDS=""

if [ -z "${JWT_SECRETO:-}" ] || [ "${#JWT_SECRETO}" -lt 32 ]; then
  echo "JWT_SECRETO must be set in Render and contain at least 32 characters." >&2
  exit 2
fi
for variable in DB_URL DB_USUARIO DB_CLAVE; do
  eval "value=\${$variable:-}"
  if [ -z "$value" ]; then
    echo "$variable must be set to connect the microservices to PostgreSQL." >&2
    exit 2
  fi
done

for jar in \
  "$EUREKA_JAR" \
  "$GATEWAY_JAR" \
  "$APP_DIR/msvc_auth-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_benchmark-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_build-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_compatibility-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_component-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_cpu-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_gpu-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_motherboard-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_power-supply-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_quotation-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_ram-service-0.0.1-SNAPSHOT.jar" \
  "$APP_DIR/msvc_user-service-0.0.1-SNAPSHOT.jar"; do
  if [ ! -f "$jar" ]; then
    echo "Required executable JAR was not found: $jar" >&2
    exit 2
  fi
done

unset AUTH_SERVICE_URL || true

stop_all() {
  exit_code="$1"
  trap - INT TERM
  for pid in $SERVICE_PIDS; do
    kill "$pid" 2>/dev/null || true
  done
  for pid in $SERVICE_PIDS; do
    wait "$pid" 2>/dev/null || true
  done
  exit "$exit_code"
}

trap 'stop_all 143' TERM
trap 'stop_all 130' INT

start_eureka() {
  echo "Starting Eureka on 127.0.0.1:8761"
  java -Xms32m "-Xmx$JAVA_XMX" "-XX:MaxMetaspaceSize=$JAVA_MAX_METASPACE" "-XX:ReservedCodeCacheSize=$JAVA_CODE_CACHE" \
    -jar "$EUREKA_JAR" \
    --server.port=8761 \
    --server.address=127.0.0.1 \
    --eureka.client.enabled=false &
  SERVICE_PIDS="$SERVICE_PIDS $!"
}

start_spring_service() {
  service_name="$1"
  jar_name="$2"
  port="$3"
  schema="$4"
  bind_address="$5"
  jar_path="$APP_DIR/$jar_name"

  echo "Starting $service_name on $bind_address:$port (schema: ${schema:-none})"
  if [ -n "$schema" ]; then
    DB_SCHEMA="$schema" java -Xms32m "-Xmx$JAVA_XMX" "-XX:MaxMetaspaceSize=$JAVA_MAX_METASPACE" "-XX:ReservedCodeCacheSize=$JAVA_CODE_CACHE" \
      -jar "$jar_path" \
      --spring.profiles.active=prod \
      --server.port="$port" \
      --server.address="$bind_address" \
      --eureka.client.enabled=true \
      --eureka.client.register-with-eureka=true \
      --eureka.client.fetch-registry=true \
      --eureka.client.service-url.defaultZone="$EUREKA_URL" \
      --eureka.instance.prefer-ip-address=true \
      --eureka.instance.ip-address=127.0.0.1 &
  else
    java -Xms32m "-Xmx$JAVA_XMX" "-XX:MaxMetaspaceSize=$JAVA_MAX_METASPACE" "-XX:ReservedCodeCacheSize=$JAVA_CODE_CACHE" \
      -jar "$jar_path" \
      --server.port="$port" \
      --server.address="$bind_address" \
      --eureka.client.enabled=true \
      --eureka.client.register-with-eureka=true \
      --eureka.client.fetch-registry=true \
      --eureka.client.service-url.defaultZone="$EUREKA_URL" \
      --eureka.instance.prefer-ip-address=true \
      --eureka.instance.ip-address=127.0.0.1 \
      --app.jwt.secreto="$JWT_SECRETO" &
  fi
  SERVICE_PIDS="$SERVICE_PIDS $!"
}

start_eureka
sleep "${EUREKA_STARTUP_WAIT_SECONDS:-20}"
if ! kill -0 "$(set -- $SERVICE_PIDS; echo "$1")" 2>/dev/null; then
  echo "Eureka stopped during startup." >&2
  stop_all 1
fi

# Start the public listener before the remaining services so Render detects PORT promptly.
start_spring_service "msvc-gateway" "msvc-gateway-0.0.1-SNAPSHOT.jar" "$GATEWAY_PORT" "" "0.0.0.0"
sleep 5

start_spring_service "msvc_auth-service" "msvc_auth-service-0.0.1-SNAPSHOT.jar" 8085 auth_service 127.0.0.1
sleep 2
start_spring_service "msvc_benchmark-service" "msvc_benchmark-service-0.0.1-SNAPSHOT.jar" 8086 benchmark_service 127.0.0.1
sleep 2
start_spring_service "msvc_build-service" "msvc_build-service-0.0.1-SNAPSHOT.jar" 8087 build_service 127.0.0.1
sleep 2
start_spring_service "msvc_compatibility-service" "msvc_compatibility-service-0.0.1-SNAPSHOT.jar" 8088 compatibility_service 127.0.0.1
sleep 2
start_spring_service "msvc_component-service" "msvc_component-service-0.0.1-SNAPSHOT.jar" 8089 component_service 127.0.0.1
sleep 2
start_spring_service "msvc_cpu-service" "msvc_cpu-service-0.0.1-SNAPSHOT.jar" 8081 cpu_service 127.0.0.1
sleep 2
start_spring_service "msvc_gpu-service" "msvc_gpu-service-0.0.1-SNAPSHOT.jar" 8082 gpu_service 127.0.0.1
sleep 2
start_spring_service "msvc_motherboard-service" "msvc_motherboard-service-0.0.1-SNAPSHOT.jar" 8083 motherboard_service 127.0.0.1
sleep 2
start_spring_service "msvc_power-supply-service" "msvc_power-supply-service-0.0.1-SNAPSHOT.jar" 8084 power_supply_service 127.0.0.1
sleep 2
start_spring_service "msvc_quotation-service" "msvc_quotation-service-0.0.1-SNAPSHOT.jar" 8092 quotation_service 127.0.0.1
sleep 2
start_spring_service "msvc_ram-service" "msvc_ram-service-0.0.1-SNAPSHOT.jar" 8090 ram_service 127.0.0.1
sleep 2
start_spring_service "msvc_user-service" "msvc_user-service-0.0.1-SNAPSHOT.jar" 8091 user_service 127.0.0.1

while :; do
  for pid in $SERVICE_PIDS; do
    if ! kill -0 "$pid" 2>/dev/null; then
      if wait "$pid"; then
        service_status=0
      else
        service_status=$?
      fi
      echo "A required service process (PID $pid) exited with status $service_status." >&2
      stop_all 1
    fi
  done
  sleep 2
done