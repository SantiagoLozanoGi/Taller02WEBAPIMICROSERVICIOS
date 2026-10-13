# VALIDATION_REPORT.md — Evidencia de ejecución y pendientes

**Fechas de verificación:** 12–13 de octubre de 2026.
**Estado general:** CÓDIGO IMPLEMENTADO / `mvn -B clean verify` **EJECUTADO Y EXITOSO** / validación con Docker pendiente.

Historial honesto del proceso: la primera ejecución del workflow en GitHub Actions falló en `mvn verify` (errores de compilación de los tests contra las APIs reales de Spring Boot 4 / Spring Security 7). Se instalaron JDK 21 (Temurin 21.0.12.1 portátil) y Maven 3.9.16 en la máquina local, se reprodujo el fallo, se diagnosticó con evidencia (bytecode con `javap`, inspección de jars) y se corrigió. La suite completa ahora pasa en local y en CI. **Ninguna prueba se reporta como ejecutada si no se ejecutó.**

---

## 1. Verificaciones EJECUTADAS (con resultado)

| # | Verificación | Herramienta | Resultado |
|---|---|---|---|
| 1 | Inventario de archivos vs. lista de la guía (A.1–A.53 + entregables) | Comparación manual | ✅ COMPLETO: 73 archivos versionados; todos los del inventario existen en las rutas correctas |
| 2 | XML bien formado (POM raíz + 3 módulos) | `System.Xml.XmlDocument` | ✅ Los 4 POM cargan sin errores |
| 3 | JSON válido (colección y ambiente Postman) | `ConvertFrom-Json` | ✅ Colección: 4 carpetas, 15 requests; ambiente: 7 variables |
| 4 | Generación local del par de llaves RSA | `sh scripts/generate-rsa-keys.sh` (OpenSSL 3.2.4) | ✅ Distribución correcta: privada+publica en auth, solo publica en users |
| 5 | Pareja de llaves coherente | `openssl rsa -pubout \| diff` | ✅ Pública derivada de la privada idéntica a la distribuida |
| 6 | Llaves fuera de Git | `git status` / `git ls-files` | ✅ Ningún `.pem` versionado; `.gitignore` efectivo |
| 7 | **`mvn -B clean verify` completo** | Temurin 21.0.12.1 + Maven 3.9.16, ejecutado localmente el 13/10/2026 | ✅ **BUILD SUCCESS**: 4 módulos, **44 pruebas, 0 fallos, 0 errores** (gateway 4, auth 17, users 23); los 3 jars ejecutables (`*-2.0.0.jar`) se empaquetan con `spring-boot:repackage` |
| 8 | Existencia real de artefactos Maven Central | Consultas HTTP a `repo1.maven.org` | ✅ Boot 4.0.8, starter-webmvc, starter-flyway, Cloud 2025.1.3, Gateway 5.0.x, springdoc 3.0.1, flyway-database-postgresql |
| 9 | Versiones vigentes de GitHub Actions | `api.github.com` | ✅ checkout v7.0.1, setup-java v6.0.1, login-action v4.6.0, setup-buildx-action v4.4.1, build-push-action v7.4.0 |
| 10 | Repositorio Git + remoto GitHub | `git push` a `main` | ✅ Publicado en https://github.com/SantiagoLozanoGi/Taller02WEBAPIMICROSERVICIOS (73 archivos) |
| 11 | Contrato de API vs implementación | Lectura cruzada + `@WebMvcTest` con seguridad real | ✅ 401/403/200/201/204/400/409/404 verificados en las clases de test |
| 12 | Compilación contra APIs reales de Boot 4 / Security 7 | `javap` sobre `spring-boot-security`, `spring-security-oauth2-jose` 7.0.7 | ✅ Evidencia de `@EnableWebSecurity` en la autoconfiguración y de la validación de issuer por claim crudo |
| 13 | **Workflow CI/CD completo en GitHub Actions** | Run #4 (commit `c5875c3`) del 13/10/2026 | ✅ **Los 4 jobs SUCCESS**: `verify` (mvn clean verify) + `docker` × 3. Imágenes publicadas y verificadas en `ghcr.io/santiagolozanogi/taller02-{api-gateway,auth-service,user-service}` con tags `latest` y SHA |
| 14 | Pasos internos de los Dockerfile | Simulación local con `.m2` limpio | ✅ `dependency:go-offline` y `clean package -DskipTests` finalizan con BUILD SUCCESS |

### 1.1 Detalle de la ejecución de `mvn -B clean verify` (13/10/2026)

```
Reactor Summary for Taller 02 - Microservices v2.0.0:
Taller 02 - Microservices ..................... SUCCESS [  0.269 s]
api-gateway .................................... SUCCESS [ 15.470 s]
auth-service ................................... SUCCESS [ 11.587 s]
user-service ................................... SUCCESS [  9.784 s]
BUILD SUCCESS
```

| Módulo | Clase de prueba | Pruebas | Resultado |
|---|---|---|---|
| api-gateway | ApiGatewayApplicationTests (contexto + rutas) | 1 | ✅ |
| api-gateway | CorrelationIdFilterTest (genera/reusa/propaga X-Correlation-Id) | 3 | ✅ |
| auth-service | AuthControllerTest (201/200/400/401/409 con seguridad real) | 6 | ✅ |
| auth-service | AuthServiceTest (BCrypt, 409 duplicados, login) | 6 | ✅ |
| auth-service | JwtServiceTest (firma RS256, claims del contrato) | 3 | ✅ |
| auth-service | CustomUserDetailsServiceTest (rol desde BD) | 2 | ✅ |
| user-service | UserControllerTest (matriz de autorización con JWT RS256 reales) | 16 | ✅ |
| user-service | UserProfileServiceTest (CRUD, 404/409, identidad del token) | 7 | ✅ |
| **Total** | | **44** | **✅ 0 fallos, 0 errores** |

### 1.2 Incidencias corregidas durante la validación (ver IMPLEMENTATION_NOTES.md §13)

1. **Run #1 y #2 de GitHub Actions fallaron** en `mvn verify` (compilación de tests): el paquete de `@WebMvcTest` cambió en Boot 4, un import faltante de `NimbusJwtEncoder`, y un varargs genérico de AssertJ. Corregido y verificado.
2. **Slice `@WebMvcTest` de Boot 4 sin autoconfiguración de seguridad:** el contexto no cargaba (`AuthenticationConfiguration`/`HttpSecurity` inexistentes). Evidencia con `javap`: la autoconfiguración `ServletWebSecurityAutoConfiguration` aplica `@EnableWebSecurity` en la app real (`@ConditionalOnMissingBean("springSecurityFilterChain")`), pero el slice no la importa. Corrección: `@TestConfiguration @EnableWebSecurity` en los tests de controlador. Verificado.
3. **Spring Security 7 y `Jwt.getIssuer()`:** el test verificaba el issuer con `getIssuer()`, que en SS7 exige URL y lanza excepción con `taller02-auth`. Evidencia bytecode: `JwtIssuerValidator` delega en `JwtClaimValidator`, que compara el claim `iss` crudo — la validación real de producción funciona con el issuer del contrato. El test se ajustó a `getClaimAsString("iss")`. **No se cambió el issuer del contrato.**

## 2. Verificaciones BLOQUEADAS (con causa y comando pendiente)

| # | Verificación de la matriz de aceptación | Causa | Comando pendiente |
|---|---|---|---|
| 1 | `docker compose config` válido | Docker no instalado | Instalar Docker Desktop y ejecutar `docker compose config` |
| 2 | `docker compose up --build -d` (5 servicios) | Docker no instalado | `docker compose up --build -d` |
| 3 | `docker compose ps` con servicios sanos | Docker no instalado | `docker compose ps` |
| 4 | Health :8080/:8081/:8082 → `{"status":"UP"}` | Servicios no levantados sin Docker | `curl http://localhost:8080/actuator/health` (y 8081, 8082) |
| 5 | Swagger en 8081/8082 | Servicios no levantados | Abrir `http://localhost:8081/swagger-ui.html` y `:8082/swagger-ui.html` |
| 6 | Login student por Gateway → 200 + accessToken | Servicios no levantados | `curl -i -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"student","password":"Student123!"}'` |
| 7 | Flujo completo POST/GET/me/PUT/DELETE por Gateway | Servicios no levantados | README §6 o colección Postman |
| 8 | Flyway: tablas `credentials`/`user_profiles` + `flyway_schema_history` | Servicios no levantados | `docker compose exec auth-db psql -U postgres -d auth_db -c "\dt"` (y users-db) |
| 9 | Persistencia tras `docker compose down` y `up` | Servicios no levantados | README §8 |
| 10 | Postman Runner completo | Servicios no levantados | Importar `postman/*.json` y ejecutar Runner |
| 11 | End-to-end con bases de datos reales | Servicios no levantados | `docker compose up` + pruebas HTTP |

> La cobertura de la matriz de aceptación que no requiere Docker ya está **verificada por la suite de 44 pruebas**: 401 sin token, 403 USER vs 200 ADMIN, 409 duplicados, 400 validación, 404 inexistente, firma/expiración/issuer de JWT, identidad desde el token.

## 3. Matriz de aceptación — estado

| Prueba | Estado |
|---|---|
| Estructura del proyecto (POM raíz + 3 módulos, Java 21, Boot 4.0.8, Cloud 2025.1.3) | ✅ Compila y empaqueta (BUILD SUCCESS local) |
| api-gateway 8080: rutas, `X-Correlation-Id`, health | ✅ Implementado y probado por tests (4/4) |
| auth-service 8081: registro, login, BCrypt, roles, JWT RS256, DTOs, validaciones, errores, Swagger | ✅ Implementado y probado por tests (17/17) |
| user-service 8082: Resource Server, llave pública, CRUD, propietario/ADMIN | ✅ Implementado y probado por tests (23/23) |
| `mvn -B clean verify` sin errores | ✅ **EJECUTADO localmente: BUILD SUCCESS, 44/44** |
| PostgreSQL independientes + Flyway V1 + `ddl-auto: validate` | ✅ Migraciones listas; ⏳ runtime con Docker |
| Dockerfiles × 3 + Compose (5 servicios, healthchecks, 2 volúmenes) | ✅ Implementados; ⏳ `docker compose config/up` |
| Llaves RSA locales + distribución correcta + ninguna privada en Git | ✅ Verificado (script, pareja, Git limpio) |
| Usuarios demo ADMIN/USER | ✅ Implementados; ⏳ runtime |
| Colección + ambiente Postman | ✅ Reconstruidos y JSON válidos; ⏳ Runner |
| Workflow CI/CD (verify + GHCR) | ✅ **EJECUTADO Y EXITOSO** (run #4): los 4 jobs SUCCESS y las 3 imágenes publicadas/verificadas en `ghcr.io/santiagolozanogi/` |
| README reproducible | ✅ Implementado |
| IMPLEMENTATION_NOTES.md + VALIDATION_REPORT.md | ✅ Este documento |

## 4. Riesgos técnicos abiertos

1. **Namespace de rutas del Gateway** (`spring.cloud.gateway.server.webflux.routes`): el contexto del gateway arrancó correctamente con la configuración (test de contexto 1/1 ✅); la confirmación de que las rutas enrutan tráfico real queda para `docker compose up`.
2. **End-to-end con bases de datos reales:** las pruebas de rebanada no usan PostgreSQL; Flyway y JPA `validate` se comprueban al levantar los contenedores.
3. **Imágenes GHCR sin llaves:** por diseño de seguridad las imágenes publicadas no contienen `.pem`; requieren inyección de llaves al desplegar (documentado en §10 de IMPLEMENTATION_NOTES.md).

## 5. Cómo completar la validación restante

```bash
# 1. Instalar Docker Desktop (las llaves RSA ya están generadas)
# 2. En la raíz del proyecto:
docker compose config
docker compose up --build -d
docker compose ps                                            # 5 servicios Up / healthy
curl http://localhost:8080/actuator/health                   # {"status":"UP"}
# 3. Pruebas funcionales: README §6 (curl) o colección Postman
# 4. Persistencia: README §8
```

El proyecto tiene todo el código compilado, las 44 pruebas de lógica/autorización/API en verde (localmente y en el runner de GitHub), la infraestructura completa y la documentación; los ítems ⏳ dependen únicamente de Docker Desktop.
