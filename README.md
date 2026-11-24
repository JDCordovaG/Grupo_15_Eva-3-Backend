# BuildMyPC - Backend

Backend de BuildMyPC implementado como una arquitectura de microservicios con Spring Boot. Incluye descubrimiento de servicios con Eureka, un Gateway para exponer la API, autenticación con JWT y servicios de dominio para cuentas, componentes, builds, compatibilidad, benchmarks y cotizaciones.

## Integrantes

- Juan David Córdova
- Vicente Pizarro
- Ignacio Castro

## Requisitos previos

- Java 21 (JDK).
- Apache Maven 3.9 o superior.
- Puertos locales disponibles: `8761`, `8080` y `8081` a `8092`.
- PostgreSQL no es necesario para la ejecución local predeterminada, que usa H2.

## Instalación y ejecución local

1. Clona el repositorio y entra en su carpeta raíz:

   ```bash
   git clone https://github.com/JDCordovaG/Grupo_15_Eva-3-Backend.git
   cd Grupo_15_Eva-3-Backend
   ```

2. Inicia cada módulo en una terminal independiente. Primero inicia Eureka y espera a que esté disponible; luego inicia los servicios de dominio y deja el Gateway para el final. Ejecuta los comandos desde la raíz del repositorio:

   ```bash
   mvn -pl msvc-eureka spring-boot:run
   mvn -pl msvc_auth-service spring-boot:run
   mvn -pl msvc_benchmark-service spring-boot:run
   mvn -pl msvc_build-service spring-boot:run
   mvn -pl msvc_compatibility-service spring-boot:run
   mvn -pl msvc_component-service spring-boot:run
   mvn -pl msvc_cpu-service spring-boot:run
   mvn -pl msvc_gpu-service spring-boot:run
   mvn -pl msvc_motherboard-service spring-boot:run
   mvn -pl msvc_power-supply-service spring-boot:run
   mvn -pl msvc_quotation-service spring-boot:run
   mvn -pl msvc_ram-service spring-boot:run
   mvn -pl msvc_user-service spring-boot:run
   mvn -pl msvc-gateway spring-boot:run
   ```

   Cada comando se ejecuta en una terminal distinta y permanece activo mientras el servicio está funcionando.

3. Comprueba el Gateway en `http://localhost:8080/actuator/health`, Eureka en `http://localhost:8761` y la API de componentes en `http://localhost:8080/api/v1/components`.

### Puertos locales predeterminados

| Servicio | Módulo Maven | Puerto |
|---|---|---:|
| Eureka | `msvc-eureka` | 8761 |
| Gateway | `msvc-gateway` | 8080 |
| CPU | `msvc_cpu-service` | 8081 |
| GPU | `msvc_gpu-service` | 8082 |
| Placa madre | `msvc_motherboard-service` | 8083 |
| Fuente de poder | `msvc_power-supply-service` | 8084 |
| Autenticación | `msvc_auth-service` | 8085 |
| Benchmark | `msvc_benchmark-service` | 8086 |
| Builds | `msvc_build-service` | 8087 |
| Compatibilidad | `msvc_compatibility-service` | 8088 |
| Componentes | `msvc_component-service` | 8089 |
| RAM | `msvc_ram-service` | 8090 |
| Usuarios | `msvc_user-service` | 8091 |
| Cotizaciones | `msvc_quotation-service` | 8092 |

## Configuración

- El Frontend consume el Gateway en `http://localhost:8080/api` por defecto.
- En desarrollo, los servicios usan bases H2 locales en `data/` y registran sus instancias en Eureka (`http://localhost:8761/eureka/`).
- Para desarrollo local, el Gateway permite por defecto el origen `http://localhost:5173`. Se puede configurar con `CORS_ORIGENES`.
- Auth firma JWT y el Gateway valida los tokens. La duración configurada es de una hora. Para entornos desplegados, configura un `JWT_SECRETO` aleatorio de al menos 32 bytes en Auth y Gateway.
- Consulta [DEPLOYMENT.md](DEPLOYMENT.md) para las variables de producción, el despliegue Docker en Render y la URL del Gateway que debe configurar Vercel. La arquitectura completa requiere varios procesos; revisa el límite de horas del plan gratuito antes de publicarla.

## Estructura de carpetas

```text
.
├── src/
│   ├── main/java/              # Clase de arranque Maven padre
│   └── test/java/              # Prueba de contexto del proyecto padre
├── msvc-eureka/                 # Registro y descubrimiento de servicios
├── msvc-gateway/                # API Gateway, CORS y validación JWT
├── msvc_auth-service/           # Autenticación y tokens
├── msvc_user-service/           # Usuarios y roles
├── msvc_component-service/      # Catálogo general de componentes
├── msvc_cpu-service/            # Componentes CPU
├── msvc_gpu-service/            # Componentes GPU
├── msvc_motherboard-service/    # Placas madre
├── msvc_ram-service/            # Memorias RAM
├── msvc_power-supply-service/   # Fuentes de poder
├── msvc_build-service/          # Builds guardadas y revisiones
├── msvc_compatibility-service/  # Validación de compatibilidad
├── msvc_benchmark-service/      # Benchmarks
├── msvc_quotation-service/      # Cotizaciones
├── data/                        # Bases H2 locales de desarrollo
├── pom.xml                      # Proyecto Maven padre y módulos
└── Dockerfile                   # Imagen Docker para los servicios
```

Cada microservicio tiene su propio `pom.xml`, configuración en `src/main/resources` y código bajo `src/main/java`. Los paquetes del dominio organizan clases de configuración, controladores, excepciones, modelos, repositorios y servicios.
