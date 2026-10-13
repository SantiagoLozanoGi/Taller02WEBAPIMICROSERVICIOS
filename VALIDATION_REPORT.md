# VALIDATION_REPORT.md — Evidencia de ejecución y pendientes

**Fechas:** implementación 12–13/10/2026; **validación integral con Docker: 13/10/2026**.
**Estado final: TODAS LAS VERIFICACIONES EJECUTABLES APROBADAS** — `mvn verify` (44/44), 5/5 servicios, health checks, Flyway, matriz funcional HTTP (40/40), seguridad, Gateway, Swagger, Postman (15/15), persistencia y CI/CD real (runs en verde).

**Entorno de la validación:** Windows 11, Java 21 (Temurin 21.0.12.101, `JAVA_HOME` de sistema), Maven 3.9.16 (`C:\Program Files\Apache\...`), Docker Engine 29.8.2 + Compose v5.6.0 (Docker Desktop/WSL2, instalación por usuario). Las herramientas no estaban en el PATH de la sesión del agente (heredado de antes de la instalación): se usaron sus rutas absolutas.

---

## 1. Matriz de validación ejecutada (13/10/2026)

| # | Prueba | Comando | Esperado | Obtenido | Estado | Evidencia / Correcciones |
|---|--------|---------|----------|----------|--------|--------------------------|
| 1 | Herramientas | `java -version`, `mvn -version`, `docker --version`, `docker compose version` | Java 21, Maven, Docker | Temurin 21.0.12.101, Maven 3.9.16, Docker 29.8.2, Compose 5.6.0 | APROBADO | Se localizaron en PATH de registro; sesión heredada sin PATH nuevo |
| 2 | Compilación + tests | `mvn -B clean verify` | BUILD SUCCESS | BUILD SUCCESS; 4+17+23 = **44 pruebas, 0 fallos, 0 errores** | APROBADO | 3 jars empaquetados con repackage |
| 3 | Config Compose | `docker compose config --quiet` | exit 0 | exit 0 | APROBADO | — |
| 4 | Levantar servicios | `docker compose up --build -d` | 5 servicios | 5/5 Up; BDs `(healthy)` | APROBADO | — |
| 5 | Estado contenedores | `docker compose ps` | 5 activos | api-gateway 8080, auth 8081, users 8082, auth-db 5433, users-db 5434 | APROBADO | — |
| 6 | Logs sin errores | `docker compose logs` (3 servicios) | Sin ERROR/FATAL | Sin ` ERROR `/`FATAL`/`Exception` | APROBADO | — |
| 7 | Health Gateway | `GET :8080/actuator/health` | 200, UP | 200 `{"groups":["liveness","readiness"],"status":"UP"}` | APROBADO | — |
| 8 | Health Auth | `GET :8081/actuator/health` | 200, UP | 200, status UP | APROBADO | — |
| 9 | Health Users | `GET :8082/actuator/health` | 200, UP | 200, status UP | APROBADO | — |
| 10 | Flyway auth_db | `docker compose logs auth-service` | "Migrating schema ... 1 - create credentials" + applied | Aplicada: `Successfully applied 1 migration ... now at v1` | APROBADO | PostgreSQL 17.11 |
| 11 | Flyway users_db | `docker compose logs user-service` | "1 - create user profiles" applied | Aplicada, mismo patrón | APROBADO | — |
| 12 | Tablas auth_db | `psql -d auth_db -c "\dt"` | credentials + flyway_schema_history | Ambas presentes | APROBADO | — |
| 13 | Tablas users_db | `psql -d users_db -c "\dt"` | user_profiles + flyway_schema_history | Ambas presentes | APROBADO | — |
| 14 | Separación de BDs | SQL `information_schema.tables` en ambas | auth_db sin user_profiles; users_db sin credentials | 0 y 0 tablas cruzadas | APROBADO | Database-per-Service confirmado |
| 15 | Hashes BCrypt | SQL `credentials` | `$2a$10$...`, 60 chars, no texto plano | admin y student con `$2a$10$` (60) | APROBADO | — |
| 16 | JPA validate | arranque de ambos servicios sin error de esquema | Sin SchemaManagementException | Servicios Started sin errores | APROBADO | — |
| 17 | Registro nuevo usuario | POST `/api/auth/register` (usuario único) | 201 | 201 + userId | APROBADO | — |
| 18 | Registro duplicado | ídem repetido | 409 | 409 "El nombre de usuario ya está registrado" | APROBADO | — |
| 19 | Registro inválido | username corto/email inválido/password débil | 400 | 400 con validationErrors por campo | APROBADO | — |
| 20 | Login ADMIN | POST `/api/auth/login` admin | 200 + JWT | 200, accessToken (623 car.), role=ADMIN | APROBADO | — |
| 21 | Login USER | ídem student | 200 + JWT | 200, accessToken (627 car.), role=USER | APROBADO | — |
| 22 | Credenciales incorrectas | password inválida | 401 | 401 "Credenciales inválidas" | APROBADO | — |
| 23 | JWT RS256 + claims | decodificar header/payload | RS256, kid, sub UUID, username, email, roles, iss | RS256/kid=taller02-key; sub=UUID; roles=["ROLE_USER"]; iss=taller02-auth; sin password/hash | APROBADO | Tokens nunca impresos completos |
| 24 | JWT alterado | payload reescrito, firma original | 401 | 401 | APROBADO | — |
| 25 | JWT expirado | firmado localmente (exp hace 1 h) | 401 | 401 | APROBADO | Firma generada con la llave local, no impresa |
| 26 | Crear perfil (USER) | POST `/api/users` con JWT student | 201 | 201, id = sub del token | APROBADO | — |
| 27 | /me | GET `/api/users/me` con JWT | 200, identidad del token | 200, id == userId del login | APROBADO | — |
| 28 | /me sin token | GET `/api/users/me` | 401 | 401 | APROBADO | — |
| 29 | Listar como USER | GET `/api/users` con student | 403 | 403 | APROBADO | — |
| 30 | Listar como ADMIN | GET `/api/users` con admin | 200 lista | 200, array | APROBADO | — |
| 31 | Consultar por ID (ADMIN) | GET `/api/users/{id}` admin | 200 | 200 | APROBADO | — |
| 32 | Consultar por ID (propietario) | ídem con student (su id) | 200 | 200 | APROBADO | — |
| 33 | Actualizar propio (USER) | PUT su id | 200 | 200, lastName actualizado | APROBADO | — |
| 34 | USER actualiza ajeno | PUT id del admin con student | 403 | 403 | APROBADO | Corrección al *script de validación*: body válido (la validación 400 precede a la seguridad de método; ver §3) |
| 35 | ADMIN actualiza de otro | PUT id de student con admin | 200 | 200 | APROBADO | — |
| 36 | Perfil duplicado | POST `/api/users` repetido | 409 | 409 "El perfil del usuario ya existe" | APROBADO | — |
| 37 | Perfil inexistente | GET id aleatorio como ADMIN | 404 | 404 | APROBADO | — |
| 38 | Perfil inválido | firstName vacío, phone inválido | 400 | 400 con validationErrors | APROBADO | — |
| 39 | DELETE como USER | DELETE su id con student | 403 | 403 | APROBADO | — |
| 40 | DELETE como ADMIN | DELETE con admin | 204 | 204; después GET → 404 | APROBADO | — |
| 41 | Llaves en jars | `grep -a 'keys/*.pem'` dentro de los contenedores | auth: private+public; users: solo public | auth-service: private.pem+public.pem; user-service: solo public.pem | APROBADO | Distribución verificada en los jars en ejecución |
| 42 | Secretos en Git | `git ls-files`, historial | Sin .pem/.env | Ninguno versionado en el árbol ni en el historial | APROBADO | — |
| 43 | Respuestas sin datos sensibles | inspección de JSON | Sin password/hash | register/login//me sin campos sensibles | APROBADO | — |
| 44 | Ruta `/api/auth/**` | Todos los tests de auth vía :8080 | Enrutable | 200/201/400/401/409 correctos | APROBADO | — |
| 45 | Ruta `/api/users/**` | Todos los tests de perfiles vía :8080 | Enrutable | 200/201/204/400/401/403/404/409 correctos | APROBADO | — |
| 46 | X-Correlation-Id del cliente | header propio en request | Devuelto igual | `corr-prueba-123` | APROBADO | — |
| 47 | X-Correlation-Id generado | request sin header | UUID nuevo en respuesta | `66350d37-...` generado | APROBADO | Propagación a servicios cubierta por tests unitarios del filtro |
| 48 | Swagger Auth | GET `:8081/swagger-ui.html` + `/v3/api-docs` | 200 + paths register/login | 200 HTML; paths `/api/auth/register`, `/api/auth/login`; bearerAuth | APROBADO | — |
| 49 | Swagger Users | GET `:8082/swagger-ui.html` + `/v3/api-docs` | 200 + CRUD | 200; `/api/users` (get,post), `/api/users/{id}` (get,put,delete), `/api/users/me` (get); bearerAuth | APROBADO | — |
| 50 | Colección Postman | `newman run ... -e ...` | 15/15 requests | **15 requests, 28 aserciones, 0 fallos**; scripts guardan studentToken/studentId/adminToken/adminId | APROBADO | Corrección aplicada a la colección: ver §3 |
| 51 | Persistencia (SQL) | estado pre/post `docker compose down` (sin -v) + `up -d` | Datos intactos | 5 credenciales antes = después; perfil "Persistente Tras Restart" intacto con mismo UUID; Flyway no reaplicó (historial V1=1) | APROBADO | Nunca se ejecutó `down -v` |
| 52 | Persistencia (API/JWT) | login post-reinicio + token pre-reinicio contra `/me` | Ambos válidos | Login 200; token PRE-reinicio → 200 con el perfil persistido | APROBADO | Stateless + llaves estables |
| 53 | CI/CD (workflow) | revisión estática de `ci-cd.yml` | Java 21, verify, push→main, GHCR, mínimo privilegio | Conforme (setup-java v6 temurin 21, mvn verify, `if` push main, `needs: verify`, `contents: read` + `packages: write`, dueño en minúsculas) | APROBADO | — |
| 54 | CI/CD (ejecución real) | API pública de GitHub Actions (solo lectura) | Runs en verde | Runs #4 y #5: **success** (verify + 3 imágenes); imágenes verificadas en `ghcr.io/santiagolozanogi/taller02-*` con tags `latest` y SHA | APROBADO | Sin acciones remotas durante la validación (ningún push) |

## 2. Resultados agregados

| Suite | Total | Aprobados | Estado |
|---|---|---|---|
| Pruebas Maven (unitarias + rebanada web) | 44 | 44 | ✅ |
| Matriz funcional HTTP (script Node por Gateway) | 40 | 40 | ✅ |
| Colección Postman (newman: solicitudes) | 15 | 15 | ✅ |
| Aserciones de la colección Postman | 28 | 28 | ✅ |
| Matriz de aceptación de la guía | 22 ítems | todos los ejecutables | ✅ |

## 3. Incidencias y correcciones durante esta validación

1. **Colección Postman — orden de ejecución (CORRECCIÓN al entregable).** Al ejecutarla con newman se detectó que el DELETE del perfil (antes 03.3) corría antes del caso negativo "perfil duplicado → 409" (04.5): el borrado destruía el perfil y ese caso recibiría 201. La guía define el DELETE como paso **opcional y final**. Corrección mínima: el DELETE pasó a ser **04.7, al final de la colección**. Verificado con newman: 15/15 y 28/28 aserciones.
2. **Script de validación — caso "perfil ajeno" (corrección al script propio, no al proyecto).** Con body inválido (teléfono "1") el 400 de validación de Spring MVC precede al 403 de seguridad de método. Con body válido se obtiene el 403 del contrato. Comportamiento documentado como nota de la fase 6.
3. **Script de validación — errores propios** (URL de lectura de llave y re-ejecutabilidad): corregidos; no afectan al repositorio.
4. **Limpieza de artefacto de validación:** se eliminó por SQL el único registro `carlos.demo` creado por la primera corrida del script de pruebas para permitir la ejecución diseñada de la colección Postman (su caso 01.1 espera 201). No se tocó ningún dato preexistente ni demo (admin/student intactos).
5. **Datos residuales de la validación:** en `auth_db` quedan usuarios de prueba creados por la sesión (`carlos.demo.*`) y en `users_db` el perfil de student "Persistente Tras Restart". Son datos demo académicos desechables; para limpiarlos por completo se requiere `docker compose down -v` (NO ejecutado por regla de no destrucción sin autorización).

## 4. Pendientes / no ejecutables en este entorno

| Ítem | Motivo | Nota |
|---|---|---|
| Ejecución del workflow en GitHub tras este commit de documentación | Requiere push (no autorizado en esta misión) | El workflow ya está validado con runs #4/#5 reales en verde; el repo local tiene las correcciones de la colección Postman listas para el próximo push |
| `docker compose down -v` / limpieza total de datos demo | Requiere autorización explícita | Regla de no destrucción |

## 5. Conclusión

Todas las verificaciones obligatorias de la matriz de aceptación del taller se ejecutaron **de verdad** y **pasaron**, con evidencia verificable en esta sesión: compilación, 44 pruebas automatizadas, 5 servicios, health checks, Flyway en ambas bases con separación real, matriz funcional HTTP completa (401/403/404/409/400/201/200/204), seguridad RSA/BCrypt auditada hasta dentro de los jars, Swagger en ambos servicios, colección Postman reproducible con newman, persistencia tras reinicio y CI/CD comprobado en GitHub Actions con imágenes publicadas. El proyecto queda **COMPLETAMENTE VALIDADO** en lo ejecutable localmente; los únicos pendientes son acciones que requieren autorización (push de la documentación actualizada y limpieza de volúmenes demo).
