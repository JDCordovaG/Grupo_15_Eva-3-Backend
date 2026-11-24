# Publicación de BuildMyPC

Esta configuración despliega el frontend en Vercel y los 12 microservicios de dominio más el Gateway en Render. Los repositorios de ejemplo de compañeros no forman parte del proceso.

## 1. Publicar el frontend en Vercel

Conecta el repositorio `Grupo_15_Eva-3-Frontend` en Vercel, con la carpeta raíz del proyecto (`.`). Usa:

- Framework: Vite
- Build command: `npm run build`
- Output directory: `dist`
- Variable de producción `VITE_API_URL`: `https://buildmypc-g15-gateway.onrender.com/api`

Puedes desplegar Vercel primero: el dominio del Gateway queda reservado por el nombre del Blueprint y Render lo pondrá en línea después. Si Render exige cambiar el slug por disponibilidad, actualiza `VITE_API_URL` con el nuevo dominio y vuelve a desplegar Vercel. Anota el dominio de producción de Vercel para el paso 3.

## 2. Aplicar el Blueprint de Render

1. Confirma que los cambios de este backend estén en el remoto GitHub y en la rama que usarás para Render. Render lee `render.yaml` desde el remoto, no desde tu disco.
2. En Render, crea un Blueprint desde el repositorio `Grupo_15_Eva-3-Backend`. El archivo `render.yaml` crea un Postgres y 13 Web Services.
3. En el valor solicitado para `CORS_ORIGENES`, pega el origen exacto de Vercel, por ejemplo `https://buildmypc.vercel.app`, sin barra final ni ruta. Render generará `JWT_SECRETO` y `INTERNAL_SERVICE_TOKEN` en un grupo compartido.
4. En cada servicio confirma **Language: Docker**, **Compute: Free ($0/month)**, Dockerfile Path `./Dockerfile` y contexto `.`. El Blueprint ya establece esos valores.
5. Aplica y espera a que los servicios reporten estado Live. El Gateway tiene health check en `/actuator/health`.

El Dockerfile de la raíz compila todos los módulos Maven y selecciona el JAR de cada servicio mediante `SERVICE_JAR`. Mantén Root Directory vacío (raíz del repositorio). No configures un servicio por subcarpeta.

### Si aparece `SERVICE_JAR must identify the executable Spring Boot JAR`

El contenedor inició sin recibir la variable `SERVICE_JAR`. En Render, cada Web Service debe tener el valor correspondiente en **Environment**. Si creaste los servicios manualmente, agrégala servicio por servicio; si usas el Blueprint, sincroniza el `render.yaml` actualizado y confirma que sus variables se aplicaron. Luego despliega el último commit.

No basta con un único servicio llamado “Backend”: el repositorio contiene varios JAR ejecutables. El Gateway requiere `msvc-gateway-0.0.1-SNAPSHOT.jar`; los demás servicios requieren su propio JAR según la tabla de abajo. El Dockerfile ahora reporta si falta la variable o si el archivo indicado no existe en la imagen.

## 3. Conexión Vercel ↔ Render

El Gateway está previsto en `https://buildmypc-g15-gateway.onrender.com`. Cuando esté Live:

1. Asegúrate de que la variable de Vercel `VITE_API_URL` sea `https://<gateway>.onrender.com/api`.
2. Asegúrate de que la variable de Render `CORS_ORIGENES` contenga el dominio de producción de Vercel, sin barra final.
3. Si cambias alguna variable, vuelve a desplegar el servicio que la consume.

El navegador llama solo al Gateway. El Gateway enruta cada API y añade un token interno; Feign envía ese mismo token en las llamadas entre microservicios. Así, aunque los servicios gratuitos tengan URL pública, sus endpoints `/api/**` rechazan llamadas directas sin el token. Los endpoints de salud y documentación OpenAPI permanecen disponibles para Render y Swagger.

## Persistencia y límites del plan gratuito

El Blueprint provisiona un único PostgreSQL Free y crea un esquema separado por microservicio. El almacenamiento local H2 queda solo para desarrollo; en Render cada servicio usa PostgreSQL y su filesystem efímero no se necesita para guardar datos.

Render entrega 750 horas gratuitas por workspace y mes, compartidas entre sus servicios gratuitos. Los servicios se suspenden al agotarlas. Los Web Services gratuitos duermen tras 15 minutos sin tráfico, tardan cerca de un minuto en despertar y pueden reiniciarse; una primera navegación puede demorar o vencer en servicios que se despierten en cadena. El plan Free entrega 512 MB de RAM por servicio. El Dockerfile limita la memoria de la JVM para reducir el riesgo de OOM, aunque la capacidad final depende del uso real.

El PostgreSQL Free de Render tiene 1 GB y expira 30 días después de crearse; Render elimina sus datos si no se actualiza antes de vencer el periodo de gracia. Es adecuado para una demostración temporal, no para conservar datos de forma permanente. Antes de vencer, exporta la base o migra a un PostgreSQL persistente.

## Comprobación después del despliegue

- Gateway: `https://<gateway>.onrender.com/actuator/health`
- Swagger del Gateway: `https://<gateway>.onrender.com/docs/swagger-ui.html`
- API de componentes: `https://<gateway>.onrender.com/api/v1/components`
- Frontend: abre el dominio de Vercel, recarga una ruta interna y prueba una operación que consulte la API.

Si una operación devuelve 503/502, revisa los logs del Gateway y del servicio de destino, el estado de Render de ambos servicios, y que la URL del Gateway en Vercel y el origen CORS de Render coincidan con los dominios reales.
