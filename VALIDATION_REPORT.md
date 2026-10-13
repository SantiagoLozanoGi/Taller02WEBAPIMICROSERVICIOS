# VALIDATION_REPORT.md — Evidencia de ejecución y pendientes

**Fecha de la verificación local:** 12 de octubre de 2026.
**Estado general:** CÓDIGO IMPLEMENTADO / VALIDACIÓN PARCIAL.

La máquina donde se implementó el proyecto no tiene JDK 21, Maven ni Docker (Java local: 1.8.0_111). Se ejecutaron todas las verificaciones posibles sin esas herramientas y se listan honestamente las que quedan bloqueadas, con su causa y el comando exacto para ejecutarlas. **Ninguna prueba se reporta como ejecutada si no se ejecutó.**

---

## 1. Verificaciones EJECUTADAS (con resultado)

| # | Verificación | Comando usado | Resultado |
|---|---|---|---|
| 1 | Inventario de archivos vs. lista de la guía (A.1–A.53 + entregables) | Comparación manual carpeta por carpeta | ✅ COMPLETO: 71 archivos fuente versionados; todos los del inventario existen en las rutas correctas |
| 2 | XML bien formado (POM raíz + 3 módulos) | `System.Xml.XmlDocument.Load()` (PowerShell) | ✅ Los 4 POM cargan sin errores |
| 3 | JSON válido (colección y ambiente Postman) | `ConvertFrom-Json` (PowerShell) | ✅ Colección: 4 carpetas, 15 requests; ambiente: 7 variables. Un error de comilla faltante en la colección se detectó y corrigió durante la validación |
| 4 | Generación local del par de llaves RSA | `sh scripts/generate-rsa-keys.sh` (Git Bash + OpenSSL 3.2.4) | ✅ Script ejecutado sin errores; `private.pem`+`public.pem` en auth-service y solo `public.pem` en user-service |
| 5 | Pareja de llaves coherente | `openssl rsa -in private.pem -pubout \| diff - public.pem` | ✅ La pública derivada de la privada es idéntica a la distribuida; user-service usa exactamente la misma pública |
| 6 | Llaves fuera de Git | `git status --porcelain \| Select-String '.pem'` | ✅ Ningún `.pem` en staging; `.gitignore` efectivo |
| 7 | Existencia real de artefactos Maven Central | Consultas HTTP a `repo1.maven.org/maven2/**/maven-metadata.xml` (12 URLs) | ✅ Verificados: parent 4.0.8, starter-webmvc, starter-flyway, cloud-deps 2025.1.3, gateway-server-webflux 5.0.x, springdoc 3.0.1, flyway-database-postgresql |
| 8 | Versiones vigentes de GitHub Actions | `api.github.com/repos/*/releases/latest` | ✅ checkout v7.0.1, setup-java v6.0.1, login-action v4.6.0, setup-buildx-action v4.4.1, build-push-action v7.4.0 — las majors de la guía son las actuales |
| 9 | Repositorio Git | `git init -b main`, `git add -A` | ✅ Repositorio inicializado, 71 archivos en staging, commit inicial creado |
| 10 | Revisión de coherencia del contrato | Lectura cruzada del contrato (pág. 27) vs. controladores, seguridad y DTOs | ✅ Endpoints, puertos, códigos y claims exactos; identidad siempre desde el JWT |

## 2. Verificaciones BLOQUEADAS (con causa y comando pendiente)

| # | Verificación de la matriz de aceptación | Causa del bloqueo | Comando pendiente de ejecutar |
|---|---|---|---|
| 1 | `mvn -B clean verify` finaliza sin errores | No hay Maven ni JDK 21 (solo Java 8) | Instalar Temurin 21 + Maven 3.9+ y ejecutar `mvn -B clean verify` en la raíz |
| 2 | `docker compose config` válido | Docker no instalado | Instalar Docker Desktop y ejecutar `docker compose config` |
| 3 | `docker compose up --build -d` levanta los 5 servicios | Docker no instalado | `docker compose up --build -d` |
| 4 | `docker compose ps` con los 5 servicios sanos | Docker no instalado | `docker compose ps` |
| 5 | Health: `GET :8080/8081/8082 /actuator/health` → `UP` | Servicios no levantados sin Docker | `curl http://localhost:8080/actuator/health` (y 8081, 8082) |
| 6 | Swagger accesible en 8081 y 8082 | Servicios no levantados | Abrir `http://localhost:8081/swagger-ui.html` y `http://localhost:8082/swagger-ui.html` |
| 7 | Login `student` por Gateway → 200 + `accessToken` | Servicios no levantados | `curl -i -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"student","password":"Student123!"}'` |
| 8 | `POST /api/users` con token student → 201 | Servicios no levantados | Ver README §6.2 |
| 9 | `GET /api/users/me` con token → 200; sin token → 401 | Servicios no levantados | Ver README §6.2 y §6.3 |
| 10 | `GET /api/users` student → 403; admin → 200 | Servicios no levantados | Ver README §6.3 |
| 11 | Duplicados → 409; entrada inválida → 400; JWT alterado/expirado → 401 | Servicios no levantados (los tests unitarios de `UserControllerTest` ya cubren estas reglas a nivel de seguridad, pendiente su ejecución con Maven) | Colección Postman carpetas 04.* |
| 12 | Flyway: tablas `credentials` / `user_profiles` + `flyway_schema_history` | Servicios no levantados | `docker compose exec auth-db psql -U postgres -d auth_db -c "\dt"` y equivalente en users_db |
| 13 | Persistencia tras `docker compose down` y `up` | Servicios no levantados | Ver README §8 |
| 14 | Postman Runner ejecuta la colección en orden | Servicios no levantados | Importar `postman/*.json`, seleccionar ambiente "Taller 02 v2.0 - Local" y ejecutar Runner |
| 15 | GitHub Actions: verify y publicación GHCR | Requiere repositorio remoto en GitHub | `git remote add origin <url>` + `git push -u origin main`; revisar la pestaña Actions |

## 3. Matriz de aceptación — estado

| Prueba | Estado |
|---|---|
| Estructura del proyecto (POM raíz + 3 módulos, Java 21, Boot 4.0.8, Cloud 2025.1.3) | ✅ Implementado y sintácticamente validado; ⏳ compilación con Maven pendiente |
| api-gateway 8080: rutas, `X-Correlation-Id`, health | ✅ Implementado (+ test unitario del filtro); ⏳ runtime pendiente |
| auth-service 8081: registro, login, BCrypt, roles, JWT RS256, DTOs, validaciones, errores, Swagger | ✅ Implementado (+ 4 clases de test); ⏳ runtime pendiente |
| user-service 8082: Resource Server, llave pública, CRUD, propietario/ADMIN, DTOs, errores, Swagger | ✅ Implementado (+ 2 clases de test); ⏳ runtime pendiente |
| PostgreSQL independientes + Flyway V1 + `ddl-auto: validate` | ✅ Implementado (migraciones y compose listos); ⏳ runtime pendiente |
| Dockerfiles × 3 + Compose (5 servicios, healthchecks, 2 volúmenes) + .dockerignore + .env.example + .gitignore | ✅ Implementado; ⏳ `docker compose config` pendiente |
| Llaves RSA locales + distribución correcta + ninguna clave privada en Git | ✅ **Verificado** (script ejecutado, pareja comprobada, Git limpio) |
| Usuarios demo ADMIN/USER | ✅ Implementado (`DemoDataConfig`, activable/desactivable); ⏳ runtime pendiente |
| Colección + ambiente Postman | ✅ Reconstruidos y validados como JSON; ⏳ Runner pendiente |
| Workflow CI/CD + versiones de actions | ✅ Implementado; actions verificadas contra GitHub Releases; ⏳ ejecución pendiente |
| README reproducible | ✅ Implementado |
| `mvn -B clean verify` | ⏳ BLOQUEADO (sin JDK 21/Maven) |
| Pruebas locales por HTTP (curl/Postman) | ⏳ BLOQUEADO (sin Docker) |
| IMPLEMENTATION_NOTES.md + VALIDATION_REPORT.md | ✅ Este documento |

## 4. Riesgos técnicos abiertos (honestidad sobre lo no verificable)

1. **Compilación no verificada:** la suite se escribió contra las APIs de Spring Boot 4 / Spring Security 7 (`@MockitoBean`, `requestMatchers`, `NimbusJwtEncoder`, `spring-cloud-starter-gateway-server-webflux`, `spring.flyway.*`), pero sin Maven local no se ejecutó. Cualquier ajuste fino de firma que revele `mvn verify` debe registrarse aquí.
2. **Namespace de rutas del Gateway:** se usó `spring.cloud.gateway.server.webflux.routes` tal como especifica la guía para Gateway 5.x; el arranque real confirmará que las rutas se registran (el test de contexto del gateway lo cubrirá).
3. **Pruebas sin base de datos:** por diseño (runner de CI sin PostgreSQL), las pruebas son unitarias y de rebanada web; la validación end-to-end con bases de datos reales queda para `docker compose up` + Postman/curl.
4. **Sincronización de acción de GitHub:** el job `docker` publica en GHCR solo en push a `main` con `GITHUB_TOKEN` (sin credenciales adicionales); si el repositorio remoto restringe packages, será necesario ajustar permisos — la guía ya contempla esta condición.

## 5. Cómo completar la validación (guía rápida)

```bash
# 1. Instalar JDK 21 (Temurin), Maven 3.9+ y Docker Desktop
# 2. En la raíz del proyecto:
mvn -B clean verify                  # suite de pruebas de los tres módulos
docker compose config                # validación del archivo Compose
docker compose up --build -d         # 5 servicios
docker compose ps                     # todos Up / healthy
curl http://localhost:8080/actuator/health   # {"status":"UP"}  (también 8081 y 8082)
# 3. Pruebas funcionales por Gateway: README §6 o colección Postman
# 4. Publicar en GitHub: git remote add origin <url> && git push -u origin main
```

El proyecto queda con todo el código, la infraestructura, las pruebas, la documentación y las verificaciones estáticas completas; los ítems ⏳ requieren únicamente las herramientas externas indicadas.
