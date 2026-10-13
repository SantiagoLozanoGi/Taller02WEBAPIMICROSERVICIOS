# Taller 02 v2.0 — Aplicación Distribuida con Arquitectura de Microservicios

**Universidad de Ibagué — Systems Engineering Programme — Web, APIs and Microservices**

Spring Boot 4 + API REST + JWT (RS256) + PostgreSQL + Docker + Swagger + GitHub Actions.

Aplicación distribuida con separación real de responsabilidades: un **API Gateway** como punto único de entrada, un servicio de autenticación que emite JWT firmados con RSA y un servicio de perfiles que valida esos tokens como OAuth2 Resource Server. Cada servicio de negocio posee su propia base de datos PostgreSQL (patrón *Database per Service*).

| Componente    | Puerto | Responsabilidad                                    | Datos     |
|---------------|--------|----------------------------------------------------|-----------|
| api-gateway   | 8080   | Enrutamiento y `X-Correlation-Id`                   | Sin BD    |
| auth-service  | 8081   | Registro, login, BCrypt, emisión de JWT RS256       | auth_db   |
| user-service  | 8082   | CRUD de perfiles y autorización propietario/ADMIN  | users_db  |

---

## 1. Requisitos previos

- **Docker Desktop** con Docker Compose v2 (ventana de comandos: `docker --version`, `docker compose version`).
- **Git** (incluye Git Bash y OpenSSL en Windows).
- **Postman** (opcional, para la colección de pruebas).
- Opcional para ejecutar sin contenedores: **Java 21** y **Maven 3.9+**.

## 2. Preparación (solo la primera vez)

```bash
# 1. Clonar o ubicarse en la raíz del proyecto
cd taller-02-microservices

# 2. Crear el archivo de variables locales (queda ignorado por Git)
cp .env.example .env

# 3. Generar el par de llaves RSA de demostración (requiere OpenSSL, incluido en Git Bash)
sh scripts/generate-rsa-keys.sh
```

El script copia `private.pem` + `public.pem` a `auth-service/src/main/resources/keys/` y **solo** `public.pem` a `user-service/src/main/resources/keys/`. La llave privada pertenece únicamente al emisor de tokens. Los `.pem` están ignorados por Git y **nunca deben publicarse**. Sin las llaves, `auth-service` no puede iniciar.

## 3. Arranque con Docker Compose

Abra **Docker Desktop** y espere a que indique *Engine running*. Luego, en Git Bash, desde la raíz del proyecto:

```bash
docker info                     # comprobar que el daemon responde
docker compose up --build -d    # construir y levantar los 5 servicios
docker compose ps               # gateway, auth, users, auth-db y users-db activos
```

Verificar que Flyway aplicó las migraciones:

```bash
docker compose logs auth-service   # "Migrating schema ... 1 - create credentials"
docker compose logs user-service   # "Migrating schema ... 1 - create user profiles"
```

## 4. URLs de comprobación

| Recurso          | URL                                        |
|------------------|--------------------------------------------|
| API Gateway      | http://localhost:8080                      |
| Swagger auth     | http://localhost:8081/swagger-ui.html      |
| Swagger users    | http://localhost:8082/swagger-ui.html       |
| Health Gateway   | http://localhost:8080/actuator/health      |
| Health Auth      | http://localhost:8081/actuator/health      |
| Health Users     | http://localhost:8082/actuator/health      |
| API Docs JSON    | http://localhost:8081/v3/api-docs y :8082/v3/api-docs |

Las tres llamadas a `actuator/health` deben responder `{"status":"UP"}`. **Swagger se consulta directo por 8081/8082; las llamadas funcionales van siempre por el Gateway en 8080.**

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## 5. Usuarios de demostración (uso académico únicamente)

| Rol   | Usuario  | Contraseña   |
|-------|----------|--------------|
| ADMIN | admin    | `Admin123!`  |
| USER  | student  | `Student123!`|

Los crea `DemoDataConfig` al arrancar `auth-service` si no existen (`app.demo-users.enabled`). Las contraseñas solo viven en el entorno académico del taller; nunca se registran en logs ni deben reutilizarse en producción.

## 6. Prueba funcional con curl (a través del Gateway)

### 6.1 Login y obtención del token

```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"Student123!"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

echo "$TOKEN" | cut -c1-50   # debe iniciar con eyJraWQiOi...
```

### 6.2 Crear y consultar el perfil propio

```bash
# Crear el perfil del sujeto del token (201; 409 si ya existe)
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "Estudiante",
    "lastName": "Demo",
    "phone": "+57 300 111 2233"
  }'

# Consultar mi perfil: la identidad sale del JWT, no de la URL
curl -i http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### 6.3 Autorización por roles

```bash
# Sin token -> 401 Unauthorized
curl -i http://localhost:8080/api/users/me

# student (ROLE_USER) listando todos -> 403 Forbidden
curl -i http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN"

# admin (ROLE_ADMIN) listando todos -> 200 OK
curl -i http://localhost:8080/api/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**401 = no autenticado; 403 = autenticado pero sin permiso.**

### 6.4 Consultar, actualizar y eliminar por ID

```bash
# Extraer el ID de las respuestas reales (no copiar IDs de las capturas de la guía)
STUDENT_ID=$(curl -s http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

# Propietario o ADMIN consultan el perfil
curl -i http://localhost:8080/api/users/$STUDENT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Propietario o ADMIN actualizan
curl -i -X PUT http://localhost:8080/api/users/$STUDENT_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName":"Estudiante","lastName":"Demo Actualizado","phone":"3001234567"}'

# Solo ADMIN elimina
curl -i -X DELETE http://localhost:8080/api/users/$STUDENT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 6.5 Registro de un usuario nuevo

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "carlos.demo",
    "email": "carlos.demo@example.com",
    "password": "Demo12345"
  }'
```

## 7. Pruebas con Postman

1. Importe **ambos archivos** de la carpeta `postman/`:
   - `Taller_02_v2.postman_collection.json`
   - `Taller_02_v2.postman_environment.json`
2. Seleccione el ambiente **"Taller 02 v2.0 - Local"** (`baseUrl = http://localhost:8080`).
3. Ejecute la colección en orden con **Runner**:

   1. Login student → guarda `studentToken` y `studentId`
   2. Login admin → guarda `adminToken` y `adminId`
   3. POST crear perfil student (201)
   4. GET mi perfil student (200)
   5. PUT actualizar perfil student (200)
   6. GET listar perfiles como ADMIN (200)
   7. Casos negativos: sin JWT (401), USER listando (403), duplicados (409), entrada inválida (400)
   8. DELETE como ADMIN (204) — solo con datos demo

Los scripts de la colección guardan tokens e IDs automáticamente, por lo que no hay que copiar JWT a mano.

## 8. Persistencia y verificación en PostgreSQL

```bash
docker volume ls

# Tablas creadas por Flyway en auth_db
docker compose exec auth-db psql -U postgres -d auth_db -c "\dt"
# -> credentials, flyway_schema_history

docker compose exec auth-db psql -U postgres -d auth_db \
  -c "SELECT username,email,role FROM credentials;"
# -> admin, student

# Tablas creadas por Flyway en users_db
docker compose exec users-db psql -U postgres -d users_db -c "\dt"
# -> user_profiles, flyway_schema_history

docker compose exec users-db psql -U postgres -d users_db \
  -c "SELECT id,username,email,first_name,last_name,phone FROM user_profiles;"
```

**Prueba de persistencia:**

```bash
docker compose down      # los datos PERMANECEN (los volúmenes siguen)
docker compose up -d     # los datos reaparecen
# Reinicio total (¡destruye los datos del taller!):
# docker compose down -v
```

Eliminar el contenedor no elimina el volumen: los datos sobreviven mientras no se borre el volumen.

## 9. Logs y diagnóstico

```bash
docker compose logs --tail=50 api-gateway
docker compose logs --tail=50 auth-service
docker compose logs --tail=50 user-service
docker compose logs -f          # en vivo; salir con Ctrl+C (no detiene nada)
```

## 10. Ejecución sin Docker (opcional)

Requiere Java 21, Maven 3.9+ y dos PostgreSQL locales con puertos `5433` (auth_db) y `5434` (users_db) o ajustar `AUTH_DB_URL`/`USER_DB_URL`:

```bash
sh scripts/generate-rsa-keys.sh      # llaves en src/main/resources/keys
mvn -B clean verify                   # compila y ejecuta la suite de pruebas
mvn -B -pl auth-service -am spring-boot:run   # terminal 1
mvn -B -pl user-service -am spring-boot:run   # terminal 2
mvn -B -pl api-gateway -am spring-boot:run    # terminal 3
```

## 11. CI/CD con GitHub Actions

El workflow `.github/workflows/ci-cd.yml` se ejecuta en cada push/PR a `main`:

1. **verify**: Java 21 (Temurin) con caché de Maven y `mvn -B clean verify`.
2. **docker** (solo en push a main): construye y publica las tres imágenes en GitHub Container Registry (`ghcr.io/<owner>/taller02-<servicio>:latest` y `:sha`), usando `GITHUB_TOKEN`.

Las imágenes publicadas **no incluyen llaves RSA** (no se versionan por seguridad). Para ejecutar una imagen de GHCR hay que inyectar el par de llaves en tiempo de despliegue (montaje de volumen o secrets del orquestador), garantizando que `auth-service` reciba la privada y que `user-service` valide con la pública pareja.

## 12. Contrato de la API (vía Gateway :8080)

| Método | Ruta               | Acceso            | Resultado                              |
|--------|--------------------|-------------------|----------------------------------------|
| POST   | `/api/auth/register` | Público         | Crea credencial con rol USER (201)     |
| POST   | `/api/auth/login`    | Público         | Devuelve JWT Bearer RS256 (200)        |
| POST   | `/api/users`         | JWT              | Crea perfil del sujeto del token (201) |
| GET    | `/api/users/me`      | JWT              | Perfil propio (200; 401 sin token)     |
| GET    | `/api/users`         | ADMIN            | Lista perfiles (200; USER recibe 403)  |
| GET    | `/api/users/{id}`    | Propietario/ADMIN | Consulta perfil                        |
| PUT    | `/api/users/{id}`    | Propietario/ADMIN | Actualiza perfil                      |
| DELETE | `/api/users/{id}`    | ADMIN            | Elimina perfil (204)                   |

JWT con `sub` (UUID), `username`, `email` y `roles`; se validan firma RSA, expiración e issuer (`taller02-auth`). La identidad del perfil proviene del token, no del body; nunca se expone la entidad JPA ni el hash de la contraseña.

## 13. Solución de problemas frecuentes

| Problema                                | Acción                                                                                     |
|-----------------------------------------|--------------------------------------------------------------------------------------------|
| Puerto ocupado                          | Cambie el puerto externo en `docker-compose.yml` (p. ej. `8080:8080` → `8090:8080`).       |
| 401 Unauthorized                        | Confirme el encabezado `Authorization: Bearer` y que el token no expiró.                  |
| 403 Forbidden                           | El JWT es válido, pero el rol no tiene permiso para la operación.                          |
| 409 Conflict                            | Username, email o perfil ya existen. Use otros datos o limpie los volúmenes (`down -v`).   |
| Error de PostgreSQL al iniciar          | Revise `docker compose logs auth-db` / `users-db` y los healthchecks.                      |
| Swagger no abre por el Gateway          | En este taller Swagger se expone directamente en 8081 y 8082.                              |
| JWT inválido tras regenerar llaves      | Vuelva a iniciar sesión: los tokens anteriores se firmaron con otra llave.                 |
| `auth-service` no arrarga (llaves)      | Ejecute `sh scripts/generate-rsa-keys.sh` antes de `docker compose up --build`.            |
| Reinicio total                          | `docker compose down -v` y luego `docker compose up --build` (¡borra los datos demo!).     |

## 14. Consideraciones de seguridad

- Contraseñas siempre con hash **BCrypt**; nunca texto plano ni en respuestas REST.
- La llave privada RSA vive solo en el emisor (`auth-service`); el validador usa únicamente la pública.
- Validación de firma, expiración e issuer en cada request; mínimo privilegio por roles.
- `.env` y llaves `.pem` fuera de Git; en producción, secrets gestionados, HTTPS y rotación de llaves.

## 15. Estructura del proyecto

```
taller-02-microservices/
├── pom.xml                        (padre Maven, Java 21, Boot 4.0.8)
├── docker-compose.yml             (5 servicios, healthchecks, 2 volúmenes)
├── .env.example / .gitignore / .dockerignore / .gitattributes
├── scripts/generate-rsa-keys.sh
├── postman/                       (colección + ambiente reconstruidas)
├── .github/workflows/ci-cd.yml
├── api-gateway/                   (Spring Cloud Gateway, Correlation ID)
├── auth-service/                  (BCrypt + JWT RS256, Flyway, Swagger)
└── user-service/                  (Resource Server, CRUD perfiles, Flyway, Swagger)
```

Documentación adicional: `IMPLEMENTATION_NOTES.md` (decisiones y correcciones) y `VALIDATION_REPORT.md` (evidencia de ejecución y pendientes).
