# Despliegue BuildMyPC en Render

El Blueprint prioriza las funciones pedidas para la demostración en el plan gratuito: login, catálogo y administración de componentes. Crea o gestiona un Postgres compartido, el Gateway, Auth, el servicio general de componentes y los servicios técnicos para CPU, GPU, placa madre, RAM y fuente de poder. Los demás microservicios no se incluyen en este despliegue reducido.

## Recursos del Blueprint

- Todos los servicios usan Docker, `./Dockerfile`, contexto `.`, y `plan: free`.
- El Gateway existente conserva el nombre y dominio `grupo-15-eva-3-backend.onrender.com`.
- Los servicios Auth y Componentes reciben credenciales de Postgres a través de `fromDatabase`; cada servicio usa su propio esquema.
- Gateway y microservicios comparten `JWT_SECRETO` e `INTERNAL_SERVICE_TOKEN` mediante el grupo `buildmypc-shared-secrets`.
- El Gateway enruta a los dominios de Auth, Componentes y los cinco servicios de catálogo indicados en `render.yaml`.
- `CORS_ORIGENES` debe coincidir con `https://grupo-15-eva-3-frontend.vercel.app` (sin barra final).

## Aplicación

1. Publica el contenido de `render.yaml` en la rama de GitHub que se usará para el Blueprint.
2. En Render, crea o sincroniza el Blueprint desde `Grupo_15_Eva-3-Backend`. Revisa que el recurso `Grupo_15_Eva-3-Backend` se asocie al Gateway existente y que `buildmypc-core-db` se asocie al Postgres creado para esta configuración.
3. Aprueba la sincronización y espera que los ocho Web Services y Postgres queden disponibles. Cada servicio selecciona su ejecutable con `SERVICE_JAR`.
4. En Vercel, configura `VITE_API_URL` como `https://grupo-15-eva-3-backend.onrender.com/api` y vuelve a desplegar el Frontend.

## Cuentas por rol

La base de datos recién creada no trae cuentas existentes. El endpoint público de registro crea cuentas `CLIENTE`; no permite elevar el rol desde el formulario. Al iniciar Auth en Render, el bootstrap crea idempotentemente una cuenta `ADMINISTRADOR` y otra `TECNICO` con contraseñas aleatorias generadas por Render. Lee ambas contraseñas desde las variables del servicio Auth en Render y guárdalas de forma segura. No se deben habilitar roles privilegiados en el registro público.

## Límite del plan gratuito

Cada Web Service gratuito tiene recursos limitados y puede suspenderse cuando permanece inactivo. Este conjunto reduce los servicios a los que necesita despertar el flujo de catálogo, pero los siete servicios de backend y el Gateway comparten la cuota del workspace. El Postgres gratuito creado para el proyecto tiene fecha de expiración indicada por Render; exporta los datos antes de esa fecha o migra la base a un plan persistente.

## Revisión después del despliegue

- Gateway: `https://grupo-15-eva-3-backend.onrender.com/actuator/health`
- Login: `POST https://grupo-15-eva-3-backend.onrender.com/api/v1/auths/login`
- Catálogo general: `GET https://grupo-15-eva-3-backend.onrender.com/api/v1/components`
- Catálogo técnico: `GET .../api/v1/cpus`, `/api/v1/gpus`, `/api/v1/placas-madre`, `/api/v1/rams`, `/api/v1/fuentes-poder`
- Crear un componente completo desde la cuenta Admin prueba la escritura del registro general y de sus atributos técnicos.
