# Despliegue del Backend en Render (Docker)

## Arquitectura

Este repositorio es un monorepo Maven Java 21. El Dockerfile compila el reactor y empaqueta los JAR ejecutables de Eureka, el gateway y los 12 microservicios. Render debe crear una instancia por JAR, usando el mismo repositorio, rama, Dockerfile (`Dockerfile`) y contexto (`.`). En cada instancia se define `SERVICE_JAR` con el nombre de su JAR. Los JAR siguen el formato `<artifactId>-<version>.jar`; los artifactId de los servicios están en sus respectivos `pom.xml`.

Se necesitan 14 servicios en total: `eureka-server`, `msvc-gateway`, y los doce módulos con prefijo `msvc_` (Auth, Benchmark, Build, Compatibility, Component, CPU, GPU, Motherboard, Power Supply, Quotation, RAM y User). Crea Eureka y los doce microservicios como servicios privados, y el gateway como Web Service público. Usa la misma región de Render. El gateway es la única URL de backend que debe usar el frontend. El plan debe permitir que las instancias privadas reciban tráfico interno; Render indica que las instancias Free no reciben solicitudes privadas.

Para que Docker y Spring escuchen en el mismo puerto, configura `PORT=10000` en los servicios privados (si Render no lo establece automáticamente); el Web Service gateway usa el `PORT` asignado por Render. El Dockerfile expone 10000. Health check del gateway: `/actuator/health`.

## PostgreSQL persistente

Cada servicio de dominio tiene su propia base H2 local por defecto para desarrollo. En producción pasa a PostgreSQL definiendo estas variables en cada uno de los 12 microservicios:

- `DB_URL`: `jdbc:postgresql://<host>:5432/<database>?sslmode=require`
- `DB_USUARIO`: usuario de PostgreSQL
- `DB_CLAVE`: contraseña
- `DB_DRIVER`: `org.postgresql.Driver`
- `DB_DIALECT`: `org.hibernate.dialect.PostgreSQLDialect`
- `DB_SCHEMA`: esquema propio del servicio
- `H2_CONSOLE_ENABLED`: `false`

Usa el host interno de la base si PostgreSQL está en Render y comparte región. No pegues una URL `postgres://...` directamente en `DB_URL`; Spring espera una URL JDBC. Crea estos esquemas una vez en PostgreSQL antes de iniciar las apps:

```sql
CREATE SCHEMA IF NOT EXISTS auth_service;
CREATE SCHEMA IF NOT EXISTS benchmark_service;
CREATE SCHEMA IF NOT EXISTS build_service;
CREATE SCHEMA IF NOT EXISTS compatibility_service;
CREATE SCHEMA IF NOT EXISTS component_service;
CREATE SCHEMA IF NOT EXISTS cpu_service;
CREATE SCHEMA IF NOT EXISTS gpu_service;
CREATE SCHEMA IF NOT EXISTS motherboard_service;
CREATE SCHEMA IF NOT EXISTS power_supply_service;
CREATE SCHEMA IF NOT EXISTS quotation_service;
CREATE SCHEMA IF NOT EXISTS ram_service;
CREATE SCHEMA IF NOT EXISTS user_service;
```

Asigna en cada microservicio el `DB_SCHEMA` que corresponde por nombre. El gateway y Eureka no necesitan variables `DB_*`. Los datos que hoy existen solo en los archivos H2 no se migran automáticamente; el PostgreSQL nuevo inicia vacío y habría que volver a cargar los componentes y cuentas necesarias.

## Token JWT

- Auth firma tokens HS256 con una duración de una hora; el gateway verifica la firma y el vencimiento en cada solicitud.
- Configura JWT_SECRETO con el mismo valor aleatorio de al menos 32 bytes en msvc_auth-service y msvc-gateway. No publiques este secreto ni uses el fallback de desarrollo en producción.
- Las contraseñas nuevas quedan guardadas con BCrypt. Cuentas que aún tengan clave en texto plano se actualizan a BCrypt al iniciar sesión correctamente.
- El registro público asigna el rol CLIENTE; el cambio a TECNICO o ADMINISTRADOR requiere una operación autenticada y autorizada.
## Eureka y CORS

- En cada microservicio y en el gateway define `EUREKA_URL` con el Service Address interno de Eureka y `/eureka/` al final, por ejemplo `http://<host-interno-eureka>:10000/eureka/`.
- En `msvc-gateway`, define `CORS_ORIGENES` con el origen exacto del frontend Vercel, como `https://<proyecto>.vercel.app`, sin slash final. Para varios orígenes sepáralos por coma.
- La configuración conserva localhost como valor predeterminado para desarrollo.
- `management.endpoints.web.exposure.include` queda limitado a `health,info`; `health` no revela detalles.

## Orden de creación y despliegue

1. Publica este repositorio en GitHub y conéctalo a Render.
2. Crea PostgreSQL, prepara los esquemas y guarda sus credenciales.
3. Crea Eureka con `SERVICE_JAR=eureka-server-<version>.jar` y `PORT=10000`; copia su dirección interna.
4. Crea los 12 microservicios con SERVICE_JAR, PORT=10000, EUREKA_URL y variables DB_* correspondientes. En Auth, configura también JWT_SECRETO.
5. Crea el gateway público con SERVICE_JAR=msvc-gateway-<version>.jar, EUREKA_URL, el mismo JWT_SECRETO y CORS_ORIGENES.
6. Revisa `/actuator/health`, que Eureka muestre los doce servicios como UP, y que `/api/v1/components` responda JSON.
7. En Vercel, configura `VITE_API_URL=https://<gateway-publico>.onrender.com/api` para Production y Preview, y vuelve a desplegar.

Los nombres y versiones concretos de los JAR aparecen en `target/` después de una compilación Maven. No guardes claves en Git. Cambiar variables en Render requiere reiniciar o desplegar nuevamente el servicio.
