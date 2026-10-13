# FINAL_TEST_REPORT.md — Taller 02 v2.0 — Informe Final de Validación

**Proyecto:** `taller-02-microservices` v2.0.0 — Universidad de Ibagué, Web APIs and Microservices.
**Fecha de la validación integral:** 13 de octubre de 2026 (sesión con Docker operativo).
**Repositorio:** https://github.com/SantiagoLozanoGi/Taller02WEBAPIMICROSERVICIOS (rama `main`).
**Veredicto: COMPLETAMENTE VALIDADO** — toda la matriz de aceptación ejecutable fue probada con éxito y con evidencia. Pendientes solo acciones que exigen autorización (push de documentación, limpieza de datos demo).

---

## 1. Resumen ejecutivo

La aplicación distribuida del Taller 02 v2.0 (Spring Boot 4.0.8, Java 21, Spring Cloud 2025.1.3) fue **construida, desplegada con Docker y probada de extremo a extremo** en el equipo local con las herramientas ya instaladas (Java 21 Temurin, Maven 3.9.16, Docker Engine 29.8.2, Compose 5.6.0). Se ejecutaron pruebas reales —no simuladas— en cinco niveles: compilación/pruebas unitarias, infraestructura de contenedores, bases de datos/migraciones, API funcional por el Gateway (incluyendo casos negativos y ataques de JWT) y persistencia tras reinicio. Además, el pipeline de GitHub Actions ya había ejecutado en verde los mismos checks en la nube y publicado las tres imágenes en GHCR.

**Resultados agregados:**

| Suite de pruebas | Total | Aprobados |
|---|---|---|
| Maven `clean verify` (unitarias + rebanada web con seguridad real) | 44 | 44 |
| Matriz funcional HTTP contra `:8080` (script propio) | 40 | 40 |
| Colección Postman ejecutada con newman (15 solicitudes) | 15 | 15 |
| Aserciones de la colección Postman | 28 | 28 |
| Matriz de aceptación de la guía (ítems ejecutables) | 54 verificaciones | 54 |

Durante la validación se detectó y corrigió **un defecto real del entregable** (orden de la colección Postman) y se documentaron dos matices técnicos. No se destruyó ningún dato, no se reveló ningún secreto y no se realizó ninguna acción remota no autorizada.

## 2. Estado de los tres microservicios

| Servicio | Puerto | Estado | Evidencia |
|---|---|---|---|
| api-gateway | 8080 | Up, enruta `/api/auth/**` y `/api/users/**`, genera/propaga `X-Correlation-Id` | `docker compose ps` + pruebas HTTP por 8080 + headers verificados |
| auth-service | 8081 | Up, Flyway V1 aplicada, usuarios demo creados, firma JWT RS256 | logs Flyway + login 200 + JWT verificado |
| user-service | 8082 | Up, Flyway V1 aplicada, valida firma/expiración/issuer con la pública | logs Flyway + 401/403/200 según rol |
| auth-db (PostgreSQL 17.11) | 5433 | Up `(healthy)` | `docker compose ps` |
| users-db (PostgreSQL 17.11) | 5434 | Up `(healthy)` | `docker compose ps` |

Logs de los tres servicios sin `ERROR`/`FATAL`/`Exception`.

## 3. Resultado de compilación Maven

`mvn -B clean verify` → **BUILD SUCCESS**. Reactor: `Taller 02 (SUCCESS)` + `api-gateway SUCCESS` + `auth-service SUCCESS` + `user-service SUCCESS`. Los tres jars ejecutables (`*-2.0.0.jar`) se empaquetan con `spring-boot:repackage`. Resolución de dependencias verificada dos veces: en la nube (GitHub Actions) y en local.

## 4. Resultado de pruebas unitarias

**44 pruebas, 0 fallos, 0 errores, 0 omitidas**, distribuidas así:

| Módulo | Clases de prueba | Pruebas | Cobertura |
|---|---|---|---|
| api-gateway | ApiGatewayApplicationTests, CorrelationIdFilterTest | 4 | Contexto+rutas; genera/reusa/propaga el correlation id |
| auth-service | AuthControllerTest, AuthServiceTest, JwtServiceTest, CustomUserDetailsServiceTest | 17 | API 201/200/400/401/409; BCrypt; duplicados; firma RS256 y claims del contrato; rol desde BD |
| user-service | UserControllerTest, UserProfileServiceTest | 23 | Matriz de autorización 401/403/200/201/204/400/404/409 con tokens RS256 reales firmados en memoria (incluye llave no confiable → 401, expirado → 401, issuer ajeno → 401); CRUD y propiedad |

## 5. Resultado de Docker

- `docker compose config --quiet` → exit 0 (archivo válido).
- `docker compose up --build -d` → build de las tres imágenes multietapa (Maven dentro del contenedor) exitosa; 5/5 servicios Up, ambas BDs con healthcheck `healthy`.
- Los pasos internos de los Dockerfile (`dependency:go-offline`, `clean package -DskipTests`) también se validaron aparte con un repositorio Maven limpio: BUILD SUCCESS.

## 6. Pruebas de PostgreSQL

- **auth_db:** tablas `credentials` y `flyway_schema_history`; historial V1 `create credentials` con `success=t`; 5 credenciales; hashes BCrypt `$2a$10$` de 60 caracteres (jamás texto plano); usuarios demo `admin`/`ADMIN` y `student`/`USER`.
- **users_db:** tablas `user_profiles` y `flyway_schema_history`; historial V1 `create user profiles` con `success=t`; esquema idéntico a la entidad (uuid id, varchar 60/120/80/30, uniques de username/email) — coherente con el arranque sin errores bajo `ddl-auto: validate`.
- **Separación Database-per-Service:** `user_profiles` NO existe en auth_db y `credentials` NO existe en users_db (0 tablas cruzadas en `information_schema`). Ningún servicio comparte conexión.

## 7. Pruebas de JWT y seguridad

| Verificación | Resultado |
|---|---|
| Algoritmo del JWT | RS256, `kid=taller02-key` |
| Claims del contrato | `sub`=UUID, `username`, `email`, `roles=["ROLE_USER"]`, `iss=taller02-auth` |
| Token sin datos sensibles | No contiene password ni hash |
| JWT con payload reescrito (firma inválida) | 401 |
| JWT expirado (firmado localmente, exp −1 h) | 401 |
| Contraseñas en base | BCrypt (`$2a$10$`, 60 chars) |
| Llave privada | Solo dentro del jar de auth-service (verificado con `grep -a` sobre el binario); **ausente** del jar de user-service y de todo el repositorio Git (árbol e historial) |
| Llave pública | Presente en auth-service y user-service (pareja del emisor, validación efectiva comprobada) |
| Sin token en endpoint protegido | 401 (no autenticado) |
| USER en operación de ADMIN | 403 (autenticado sin permiso) |
| Propietario vs. ajeno | El dueño actualiza su perfil (200); sobre perfil ajeno recibe 403 |
| Respuestas HTTP | Ninguna expone password/hash; el login devuelve solo token/datos de perfil |
| Tokens/llaves en este informe | Nunca impresos completos (solo longitudes/prefijos) |

Nota técnica documentada: la validación del body (@Valid) en Spring MVC ocurre antes de la seguridad de método, por lo que una solicitud ajena con datos inválidos responde 400 y no 403; con datos válidos se obtiene el 403 del contrato. No es una debilidad: la autorización se aplica a toda petición bien formada.

## 8. Pruebas de CRUD (vía Gateway :8080)

| Operación | Código | Estado |
|---|---|---|
| Crear perfil propio (sujeto del token) | 201 | ✅ |
| Consultar `/me` (identidad del token) | 200 | ✅ |
| Consultar por ID propio / como ADMIN | 200 | ✅ |
| Listar todos como ADMIN | 200 (array) | ✅ |
| Listar todos como USER | 403 | ✅ |
| Actualizar perfil propio | 200 | ✅ |
| USER actualiza perfil ajeno | 403 | ✅ |
| ADMIN actualiza perfil de otro | 200 | ✅ |
| Crear perfil duplicado | 409 | ✅ |
| Consultar inexistente | 404 | ✅ |
| Crear con datos inválidos | 400 (errores por campo) | ✅ |
| DELETE como USER | 403 | ✅ |
| DELETE como ADMIN | 204 (y luego GET → 404) | ✅ |
| Sin token | 401 | ✅ |

Todos los IDs usados salieron de respuestas reales (variables de entorno/scripts), nunca de las capturas de la guía.

## 9. Pruebas de Swagger / Postman

- **Swagger Auth (`:8081`)**: UI 200 (HTML) y `/v3/api-docs` con título "Taller 02 v2.0 - Auth Service", paths `/api/auth/register` y `/api/auth/login`, esquema de seguridad `bearerAuth`.
- **Swagger Users (`:8082`)**: UI 200 y `/v3/api-docs` con paths `/api/users` (get, post), `/api/users/{id}` (get, put, delete) y `/api/users/me` (get), esquema `bearerAuth`.
- **Postman con newman 6.2.3** (instalado en la sesión): colección + ambiente importados desde `postman/`; resultado **15/15 solicitudes y 28/28 aserciones**, con scripts que guardaron automáticamente `studentToken`, `studentId`, `adminToken`, `adminId`. Tiempo total de la corrida: 1.7 s; respuesta promedio 34 ms.

## 10. Persistencia

| Paso | Resultado |
|---|---|
| Estado pre-reinicio | 5 credenciales en auth_db; perfil "Persistente Tras Restart" en users_db (UUID 9a5743f2-...) |
| `docker compose down` (SIN `-v`) | Contenedores eliminados; **volúmenes `auth-data` y `users-data` intactos** |
| `docker compose up -d` | 5/5 Up, BDs healthy; Flyway NO reaplicó la migración (historial V1=1) |
| Estado post-reinicio | 5 credenciales; mismo perfil con mismo UUID y teléfono |
| Login tras reinicio | 200 con nuevo JWT |
| **Token emitido ANTES del reinicio** | Sigue válido después: `GET /me` → 200 con el perfil persistido (stateless + llaves estables) |

Nunca se ejecutó `docker compose down -v`.

## 11. Estado de CI/CD

- **Revisión estática del workflow** (`.github/workflows/ci-cd.yml`): Java 21 Temurin con caché (`setup-java@v6`), `mvn -B clean verify`, job `docker` condicionado a push a `main` con `needs: verify`, permisos de mínimo privilegio (`contents: read` + `packages: write` en el job docker), login a GHCR con `GITHUB_TOKEN`, matriz de 3 servicios, tags `latest`+`sha` con dueño normalizado a minúsculas (corrección documentada: Docker rechaza mayúsculas en nombres de imagen).
- **Ejecución REAL en GitHub (solo lectura):** runs #4 y #5 = **success** (verify + las tres imágenes). Imágenes verificadas en el registro GHCR: `ghcr.io/santiagolozanogi/taller02-{api-gateway,auth-service,user-service}` con tags `latest` y SHA del commit.
- **Distingue validación local vs. remota:** local = `mvn verify` + toda la matriz funcional de esta sesión; remota = runs reales de Actions ya verdes e imágenes publicadas. Durante esta validación **no se realizó ningún push ni acción remota** (regla de la misión); el repositorio local acumula las correcciones de la colección Postman pendientes de push autorizado.

## 12. Errores encontrados y soluciones

| # | Error | Causa | Corrección | Re-prueba |
|---|---|---|---|---|
| 1 | La colección Postman ejecutada en orden dejaría "perfil duplicado → 409" en 201 | El DELETE del perfil corría antes del caso negativo 04.5 | Se movió el DELETE al final como **04.7** (la guía lo define como opcional y final) | newman: 15/15 y 28/28 ✅ |
| 2 | "USER actualiza perfil ajeno" obtuvo 400 en vez de 403 | El script de pruebas enviaba un teléfono inválido; en Spring MVC la validación del body precede a la seguridad de método | Script corregido con body válido; comportamiento documentado | 403 correcto ✅ (sin cambios en la aplicación) |
| 3 | Primera corrida del script abortó en la prueba de JWT expirado | Error del propio script (uso incorrecto de `new URL` para leer la llave local) | Corregido; la llave se lee con ruta directa y jamás se imprime | Corrida completa 40/40 ✅ |
| 4 | La corrida inicial de la colección habría chocado con un usuario `carlos.demo` ya existente | Ese usuario lo había creado el propio script de pruebas en su primera corrida (artefacto de validación) | Limpieza quirúrgica de ese único registro por SQL, documentada; no se tocó ningún dato preexistente | newman 01.1 → 201 ✅ |

Ninguno de los errores 2–4 implicó defectos del código de la aplicación; el único cambio al entregable fue el orden de la colección Postman (error 1). No fue necesario recompilar la aplicación durante esta validación: **el código del taller pasó todo a la primera**.

## 13. Requisitos pendientes

No hay requisitos técnicos pendientes. Quedan **dos acciones sujetas a autorización explícita**:

1. **Push de los cambios locales** (corrección de la colección Postman + `VALIDATION_REPORT.md` + este informe) al repositorio remoto — no realizado por la regla de no ejecutar acciones remotas sin permiso.
2. **Limpieza de datos demo** (`docker compose down -v`) — no ejecutada por la regla de no destrucción; los datos residuales de las pruebas (usuarios `carlos.demo.*` en auth_db y el perfil "Persistente Tras Restart") son desechables del entorno académico.

## 14. Conclusión sobre el cumplimiento del Taller 02

El proyecto cumple **completamente** los requisitos del documento original, verificado con ejecuciones reales:

- **Arquitectura:** tres componentes independientes con responsabilidades separadas y Gateway como único punto de entrada; stateless.
- **Seguridad:** BCrypt, JWT RS256 (privada solo en el emisor, pública solo en el validador — auditado dentro de los jars), validación de firma/expiración/issuer, roles USER/ADMIN, propiedad, cero secretos en Git ni en respuestas.
- **Datos:** Database-per-Service con Flyway V1 en ambas bases, `ddl-auto: validate`, persistencia demostrada tras reinicio sin borrar volúmenes.
- **API:** el contrato completo de la página 27 responde exactamente con los códigos esperados (201/200/204/400/401/403/404/409), con la identidad siempre derivada del token.
- **Observabilidad/Calidad:** `X-Correlation-Id` del Gateway, Swagger en ambos servicios, 44 pruebas automatizadas en verde, colección Postman reproducible (newman 15/15).
- **DevOps:** Docker Compose con 5 servicios sanos y healthchecks; GitHub Actions ejecutando verify y publicando las tres imágenes en GHCR (runs reales en verde).

**El Taller 02 v2.0 queda COMPLETAMENTE VALIDADO.**
