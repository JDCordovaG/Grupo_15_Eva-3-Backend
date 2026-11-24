# Despliegue BuildMyPC en Render

El Blueprint publica el Gateway y los microservicios de Auth, usuarios, builds, compatibilidad, benchmarks, cotizaciones, componentes y catálogo técnico (CPU, GPU, placa madre, RAM y fuente de poder). Eureka queda deshabilitado porque los servicios se conectan por URL directa. La aplicación usa Supabase como PostgreSQL; el Postgres `buildmypc-core-db` de Render se conserva en el Blueprint, pero no es la base activa y tiene vencimiento propio.

## Recursos del Blueprint

- Todos los servicios usan Docker, `./Dockerfile`, contexto `.`, y `plan: free`.
- El Gateway existente conserva el nombre y dominio `grupo-15-eva-3-backend.onrender.com`.
- Los servicios existentes conservan sus variables Supabase configuradas en Render. Los cinco servicios nuevos reutilizan las variables `DB_*` del servicio Auth mediante referencias `fromService`; cada servicio usa su propio esquema.
- Gateway y microservicios comparten `JWT_SECRETO` e `INTERNAL_SERVICE_TOKEN` mediante el grupo `buildmypc-shared-secrets`.
- El Gateway enruta a todos los microservicios con URLs explícitas definidas en `render.yaml`.
- `CORS_ORIGENES` debe coincidir con `https://grupo-15-eva-3-frontend.vercel.app` (sin barra final).

## Aplicación

1. Publica el contenido de `render.yaml` en la rama de GitHub que se usará para el Blueprint.
2. En Render, crea o sincroniza el Blueprint desde `Grupo_15_Eva-3-Backend`. Revisa que el recurso `Grupo_15_Eva-3-Backend` se asocie al Gateway existente y que `buildmypc-core-db` se asocie al Postgres creado para esta configuración.
3. Revisa la sincronización del Blueprint y espera que los trece Web Services queden disponibles. Cada servicio selecciona su ejecutable con `SERVICE_JAR`.
4. En Vercel, configura `VITE_API_URL` como `https://grupo-15-eva-3-backend.onrender.com/api` y vuelve a desplegar el Frontend.

## Cuentas por rol

La base de datos recién creada no trae cuentas existentes. El endpoint público de registro crea cuentas `CLIENTE`; no permite elevar el rol desde el formulario. Al iniciar Auth en Render, el bootstrap crea idempotentemente una cuenta `ADMINISTRADOR` y otra `TECNICO` con contraseñas aleatorias generadas por Render. Lee ambas contraseñas desde las variables del servicio Auth en Render y guárdalas de forma segura. No se deben habilitar roles privilegiados en el registro público.

## Límite del plan gratuito

Los Web Services gratuitos tienen recursos limitados y pueden suspenderse cuando permanecen inactivos. Las solicitudes entre servicios pueden despertar varias instancias y demorar más la primera vez. El Postgres de Render no contiene la base activa de la aplicación; las tablas y datos están en Supabase.

## Revisión después del despliegue

- Gateway: `https://grupo-15-eva-3-backend.onrender.com/actuator/health`
- Login: `POST https://grupo-15-eva-3-backend.onrender.com/api/v1/auths/login`
- Catálogo general: `GET https://grupo-15-eva-3-backend.onrender.com/api/v1/components`
- Catálogo técnico: `GET .../api/v1/cpus`, `/api/v1/gpus`, `/api/v1/placas-madre`, `/api/v1/rams`, `/api/v1/fuentes-poder`
- Builds por usuario: `GET .../api/v1/builds/usuario/{usuarioId}`
- Bandeja técnica: `GET .../api/v1/builds/pendientes-revision` y `GET .../api/v1/builds/revisadas`
- Crear un componente completo desde la cuenta Admin prueba la escritura del registro general y de sus atributos técnicos.
