# IMPLEMENTATION_NOTES.md — Decisiones y correcciones documentadas

**Proyecto:** `taller-02-microservices` v2.0.0 — Taller 02, Universidad de Ibagué.
**Fuente de requisitos:** `Taller_02_v2_Guia_Completa.docx` (transcripción íntegra en `Taller_02_Paquete_Para_Agente/AGENTS.md`, Anexos A.1–A.53).

Cada desviación respecto al documento original se lista aquí con su justificación, conforme al protocolo de correcciones mínimas.

---

## 1. Reconstrucción del código del Anexo A

El código de referencia llegó como texto convertido desde Word con caracteres escapados (`\</`, `\<`, `\>`, `\|\`, `-\>`, `\[`, etc.) e indentación destruida. **No se pegó a ciegas**: cada archivo se restauró a su sintaxis real (XML/POM, YAML, Java, Dockerfile, SQL y shell) y se revisó contra el contrato de API de la sección 3 del AGENTS.md. El resultado compila conceptualmente con las firmas de Spring Boot 4 / Spring Security 7, aunque la compilación física queda pendiente de Maven (ver `VALIDATION_REPORT.md`).

## 2. Corrección de ruta en A.13 (error tipográfico del documento)

- **Requisito original:** `auth-service/src/main/edu/unibague/auth/controller/AuthController.java`.
- **Problema:** la ruta omite el segmento `java/`; ninguna herramienta Maven compilaría ahí.
- **Solución aplicada:** `auth-service/src/main/java/edu/unibague/auth/controller/AuthController.java`. Sin cambio de paquete ni de nombre de clase.

## 3. JSON de Postman reconstruidos (Anexo B)

- **Requisito original:** el Anexo B describe dos archivos importables, pero su contenido no está incluido en el DOCX.
- **Solución aplicada:** se reconstruyeron `postman/Taller_02_v2.postman_collection.json` (15 solicitudes en 4 carpetas: autenticación, perfil del estudiante, administración ADMIN y casos negativos) y `postman/Taller_02_v2.postman_environment.json` (ambiente "Taller 02 v2.0 - Local", `baseUrl=http://localhost:8080`). Los scripts de prueba guardan automáticamente `studentToken`, `studentId`, `adminToken` y `adminId`, y las solicitudes usan esos IDs extraídos de respuestas reales.
- **Nombre del ambiente:** la guía alterna `enviroment` y `environment`; se usa `Taller_02_v2.postman_environment.json` según el inventario del documento.

## 4. Ruta del workflow de CI

- **Requisito original:** la estructura inicial muestra `github/workflows/ci-cd.yml`, pero el inventario y la sección CI/CD usan `.github/workflows/ci-cd.yml`.
- **Solución aplicada:** `.github/workflows/ci-cd.yml` (convención real de GitHub Actions y la especificada por el inventario).

## 5. UUIDs de las capturas y asignación shell inválida

- Los identificadores literales de las figuras de la guía (p. ej. `f0bf8ba7-...`, `472da599-...`) **no se copiaron** a README, Postman ni scripts: en todas partes el ID se extrae de la respuesta real (`sed` en curl, `body.id` en Postman).
- El ejemplo `ADMIN_ID= "..."` (con espacio tras el `=`) se corrigió a la asignación shell válida `ADMIN_ID="..."` en la documentación.

## 6. Llaves RSA: cero llaves en el repositorio

- **Requisito original:** par PEM entregado aparte; la privada solo al emisor, nunca en Git.
- **Solución aplicada:**
  - `scripts/generate-rsa-keys.sh` genera el par y distribuye `private.pem` + `public.pem` a auth-service y **solo** `public.pem` a user-service (A.53 restaurado).
  - **Corrección de seguridad al A.52:** el `.gitignore` original no excluía los `.pem` generados; se añadieron los patrones `auth-service/src/main/resources/keys/*.pem`, `user-service/src/main/resources/keys/*.pem` y `.keys-tmp/`. Se verificó con `git status` que ningún `.pem` queda en staging.
  - **Pruebas automatizadas sin secretos:** ninguna llave se versiona ni para tests. `TestRsaKeys` genera pares RSA en memoria y los expone como archivos temporales vía `@DynamicPropertySource`, de modo que `mvn verify` funciona en CI sin secretos externos. Para los tests de user-service, un segundo par "no confiable" permite verificar que un token firmado por un tercero recibe 401.
  - En esta máquina se generó y verificó un par de demostración local (fuera de Git) para que `docker compose up` funcione en cuanto exista Docker.

## 7. Verificación de versiones contra registros públicos (2026-10-12)

Confirmado en Maven Central y GitHub Releases que las versiones del documento existen y son vigentes (sin cambios de versión, por tanto):

| Artefacto / Action | Versión de la guía | Estado verificado |
|---|---|---|
| spring-boot-starter-parent | 4.0.8 | Publicado en Maven Central |
| spring-boot-starter-webmvc | (gestionado por Boot 4) | Existe desde 4.0.0-M1 — nombre real de Boot 4 |
| spring-boot-starter-flyway | (gestionado por Boot 4) | Existe desde 4.0.0-M1 — nombre real de Boot 4 |
| spring-cloud-dependencies | 2025.1.3 | Publicado en Maven Central |
| spring-cloud-starter-gateway-server-webflux | (gestionado por Cloud 2025.1.3, Gateway 5.x) | Línea 5.0.x publicada |
| springdoc-openapi-starter-webmvc-ui | 3.0.1 | Publicado en Maven Central |
| flyway-database-postgresql | (gestionado por Boot) | Publicado |
| actions/checkout | v7 | Última release: v7.0.1 |
| actions/setup-java | v6 | Última release: v6.0.1 |
| docker/login-action | v4 | Última release: v4.6.0 |
| docker/setup-buildx-action | v4 | Última release: v4.4.1 |
| docker/build-push-action | v7 | Última release: v7.4.0 |

La compilación y ejecución de pruebas (`mvn -B clean verify`) fue verificada posteriormente en local (Temurin 21.0.12.1 + Maven 3.9.16) y en el runner de GitHub Actions: **BUILD SUCCESS, 44/44 pruebas** (ver `VALIDATION_REPORT.md` §1.1 y §13 de este documento).

## 8. Pruebas automatizadas (añadidas; la guía exige pruebas realistas pero no las incluye)

Suite diseñada para correr sin base de datos ni secretos (compatible con el runner del workflow `verify`):

- **auth-service:** `JwtServiceTest` (firma RS256 + claims del contrato, decodificables con la pública), `AuthServiceTest` (registro, 409 por username/email duplicado, BCrypt — nunca texto plano, login), `CustomUserDetailsServiceTest`, `AuthControllerTest` (`@WebMvcTest` + seguridad real: 201/200/400/401/409 del contrato).
- **user-service:** `UserControllerTest` (`@WebMvcTest` + `SecurityConfig` real con tokens RS256 mintados en memoria: 401 sin token / llave ajena / expirado / issuer incorrecto; 403 USER listando o borrando o leyendo perfil ajeno; 200/201/204/400/409/404 en los casos positivos), `UserProfileServiceTest` (CRUD, identidad desde el token, duplicados 409, inexistente 404).
- **api-gateway:** `CorrelationIdFilterTest` (genera, reutiliza y propaga `X-Correlation-Id`) y `ApiGatewayApplicationTests` (el contexto arranca con las rutas configuradas).
- Se usó `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) porque en Spring Boot 4 es el reemplazo de `@MockBean`.

## 9. Archivos añadidos no contradictorios con la guía

- `.gitattributes`: fuerza `*.sh` a LF para que `scripts/generate-rsa-keys.sh` funcione tras un checkout en Windows (Git Bash rechaza shell con CRLF). Justificación: reproducibilidad del entregable.
- `.dockerignore`: exigido en el inventario pero sin contenido en el Anexo A; se definió excluyendo `.git`, `**/target`, `.env`, postman, docs y markdown del contexto de build.
- Tests descritos en el punto 8.
- `IMPLEMENTATION_NOTES.md` y `VALIDATION_REPORT.md` (este par de documentos, exigidos por el AGENTS.md).

## 10. Notas de despliegue de las imágenes GHCR

Las imágenes que publica el workflow **no contienen llaves RSA** (no se versionan por diseño). Para ejecutar una imagen de GHCR en otro entorno deben inyectarse `private.pem` (solo auth-service) y la `public.pem` pareja (user-service) en tiempo de despliegue (volumen o secrets). El flujo académico local (`scripts/generate-rsa-keys.sh` + `docker compose up --build`) no requiere este paso porque el build copia las llaves generadas dentro de `src/`.

## 11. Contrato de API respetado sin cambios

Tabla de endpoints, puertos, códigos (201/200/204/400/401/403/404/409), claims del JWT (`sub` UUID, `username`, `email`, `roles`), issuer `taller02-auth`, validación de firma/expiración/issuer con la pública, identidad del token (nunca del body), Swagger directo en 8081/8082 y rutas del Gateway `/api/auth/**` y `/api/users/**` con `X-Correlation-Id`: todo implementado exactamente como el contrato de la página 27, que prevalece sobre las explicaciones mezcladas de las páginas 20–21.

## 12. Comandos destructivos

`docker compose down -v`, borrado de volúmenes y DELETE de perfiles se documentaron y ejecutan **solo** con datos demo desechables; el README lo advierte explícitamente. Ningún comando destructivo se ejecutó durante esta implementación.

## 13. Correcciones de compatibilidad detectadas al ejecutar `mvn -B clean verify` (13/10/2026)

La primera ejecución del workflow en GitHub Actions falló en el paso `Compile and test`. Se reprodujo el fallo localmente (Temurin 21.0.12.1 + Maven 3.9.16) y se corrigieron, con evidencia, los siguientes puntos — **todos en las pruebas; el código principal del Anexo A no requirió cambios**:

1. **`@WebMvcTest` cambió de ubicación en Spring Boot 4.**
   - Requisito original: no aplica (la guía no incluye tests); surge de las versiones del propio documento.
   - Problema: el paquete `org.springframework.boot.test.autoconfigure.web.servlet` ya no existe en Boot 4; la anotación vive ahora en `org.springframework.boot.webmvc.test.autoconfigure` (verificado inspeccionando `spring-boot-webmvc-test-4.0.8.jar`).
   - Solución: se añadió la dependencia `org.springframework.boot:spring-boot-webmvc-test` (scope test) a `auth-service` y `user-service` y se usó el nuevo import en `AuthControllerTest` y `UserControllerTest`.

2. **El slice `@WebMvcTest` de Boot 4 no importa la autoconfiguración de seguridad.**
   - Problema: el contexto de test no cargaba porque `SecurityConfig` requiere beans `AuthenticationConfiguration` y `HttpSecurity`, provistos en la app real por `@EnableWebSecurity` dentro de `ServletWebSecurityAutoConfiguration` (verificado con `javap`: `@ConditionalOnMissingBean(name="springSecurityFilterChain")` + `@EnableWebSecurity`), que el slice no importa.
   - Solución: en ambos tests de controlador se añadió una `@TestConfiguration @EnableWebSecurity` que activa la infraestructura real de seguridad. Con ello se prueban las cadenas de filtros auténticas (los 401/403 de la matriz de aceptación se validan con la seguridad real).

3. **Spring Security 7 convierte `Jwt.getIssuer()` a URL.**
   - Problema: el test `JwtServiceTest` usaba `jwt.getIssuer()`, que en SS7 lanza `IllegalArgumentException` para el issuer del contrato (`taller02-auth`, no es URL).
   - Evidencia de que el contrato NO debe romperse: `JwtIssuerValidator` delega en `JwtClaimValidator`, que compara el claim `iss` crudo mediante `Jwt.getClaim()` (bytecode verificado con `javap`), por lo que la validación real de producción funciona con `taller02-auth`.
   - Solución: el test lee `jwt.getClaimAsString("iss")`. **El issuer de la app se mantiene en `taller02-auth` según contrato.**

4. **Timestamps de claims JWT con precisión de segundos.**
   - Problema: `Jwt.getExpiresAt()` trunca a segundos y la comparación exacta con el `Instant` (nanosegundos) fallaba.
   - Solución: la aserción compara `result.expiresAt().truncatedTo(ChronoUnit.SECONDS)`.

5. **Detalles menores de compilación:** import faltante de `NimbusJwtEncoder` en `AuthServiceTest`; `containsExactly` con varargs de tipo capturado (`Collection<? extends GrantedAuthority>`) se sustituyó por `singleElement().isEqualTo(...)` en `CustomUserDetailsServiceTest`.

**Resultado tras las correcciones:** `mvn -B clean verify` finaliza con **BUILD SUCCESS** (4 módulos, 44 pruebas: 0 fallos, 0 errores). Detalle completo en `VALIDATION_REPORT.md` §1.1.

## 14. Corrección del job `docker` del workflow (13/10/2026)

- **Requisito original (A.50):** las etiquetas de imagen usan `ghcr.io/${{ github.repository_owner }}/taller02-<servicio>`.
- **Problema observado:** el run #3 de Actions pasó `verify` pero el job `docker` de `api-gateway` falló ~15 s después del login, sin llegar a construir (sus hermanos de matriz se cancelaron por `fail-fast`). Docker exige nombres de repositorio de imagen **en minúsculas** (`invalid reference format: repository name must be lowercase`), y el dueño de este repositorio (`SantiagoLozanoGi`) contiene mayúsculas. La plantilla del documento falla para cualquier cuenta con ese formato.
- **Evidencia adicional:** los pasos internos de los Dockerfile (`dependency:go-offline` y `clean package -DskipTests`) se validaron localmente con un repositorio Maven limpio: BUILD SUCCESS, por lo que el fallo no estaba en la construcción.
- **Solución aplicada:** paso `Determine lowercase image owner` que normaliza `github.repository_owner` a minúsculas antes de componer los tags. Las imágenes se publican como `ghcr.io/santiagolozanogi/taller02-<servicio>` (GHCR asocia el espacio de nombres al mismo usuario sin distinción de mayúsculas).

## 15. Corrección de la colección Postman detectada al ejecutarla (13/10/2026, validación con newman)

- **Requisito original:** la guía describe la secuencia didáctica con el DELETE del perfil como paso **opcional y final** ("Opcionalmente ejecute DELETE como ADMIN").
- **Problema observado:** en la colección reconstruida, el DELETE (03.3) se ejecutaba antes del caso negativo "perfil duplicado → 409" (04.5), de modo que al correr la colección en orden el borrado eliminaba el perfil y 04.5 recibiría 201 en lugar de 409.
- **Solución aplicada:** el DELETE se movió al final de la colección como **04.7 "DELETE eliminar perfil como ADMIN (al final)"**, respetando el orden de la guía. Verificado con newman: 15/15 solicitudes y 28/28 aserciones.
- **Dato adicional:** durante la misma sesión se verificó que la validación del body (@Valid) en Spring MVC precede a la seguridad de método, por lo que una solicitud a recurso ajeno con datos inválidos responde 400; con datos válidos responde el 403 del contrato. Sin cambios de código.
