#!/bin/sh
set -eu

AUTH_JAR="/app/msvc_auth-service-0.0.1-SNAPSHOT.jar"
GATEWAY_JAR="/app/msvc-gateway-0.0.1-SNAPSHOT.jar"

if [ -z "${JWT_SECRETO:-}" ] || [ "${#JWT_SECRETO}" -lt 32 ]; then
  echo "JWT_SECRETO must be set in Render and contain at least 32 characters." >&2
  exit 2
fi
if [ ! -f "$AUTH_JAR" ] || [ ! -f "$GATEWAY_JAR" ]; then
  echo "Auth or Gateway executable JAR was not found in /app." >&2
  exit 2
fi

AUTH_PID=""
GATEWAY_PID=""

cleanup() {
  if [ -n "$GATEWAY_PID" ]; then
    kill "$GATEWAY_PID" 2>/dev/null || true
  fi
  if [ -n "$AUTH_PID" ]; then
    kill "$AUTH_PID" 2>/dev/null || true
  fi
  wait 2>/dev/null || true
}

trap 'cleanup; exit 143' TERM
trap 'cleanup; exit 130' INT

java -Xms32m -Xmx144m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m \
  -jar "$AUTH_JAR" \
  --server.port=8085 \
  --server.address=127.0.0.1 \
  --eureka.client.enabled=false &
AUTH_PID=$!

sleep 10
if ! kill -0 "$AUTH_PID" 2>/dev/null; then
  echo "Auth service stopped during startup." >&2
  wait "$AUTH_PID" || true
  exit 1
fi

AUTH_SERVICE_URL="http://127.0.0.1:8085" java -Xms32m -Xmx144m \
  -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m \
  -jar "$GATEWAY_JAR" \
  --server.port="${PORT:-10000}" \
  --server.address=0.0.0.0 \
  --eureka.client.enabled=false &
GATEWAY_PID=$!

while kill -0 "$GATEWAY_PID" 2>/dev/null; do
  if ! kill -0 "$AUTH_PID" 2>/dev/null; then
    echo "Auth service exited while Gateway was running." >&2
    cleanup
    exit 1
  fi
  sleep 2
done

if wait "$GATEWAY_PID"; then
  EXIT_STATUS=0
else
  EXIT_STATUS=$?
fi
cleanup
exit "$EXIT_STATUS"