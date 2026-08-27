# Vista 360° — Servicio de Registro Académico

Prueba técnica para la vacante de Ingeniero de Arquitectura e Innovación (ICESI) — **Parte 2**.

Servicio propio y autónomo que, dado el identificador de un estudiante, devuelve las
materias que tiene **matriculadas** y la **nota** registrada en cada una para un periodo
académico.

> El análisis completo de la prueba, la arquitectura de la solución (Partes 1, 3 y 4) y los
> supuestos declarados están en [`docs/00-analisis-y-propuesta.md`](docs/00-analisis-y-propuesta.md).

---

## 1. Stack

| | |
|---|---|
| Lenguaje / runtime | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security / OAuth2 Resource Server, Actuator) |
| Persistencia | PostgreSQL (producción) · H2 en memoria (local y pruebas) |
| Migraciones | Flyway |
| Build | Gradle 9.7 (wrapper incluido) |
| Pruebas | JUnit 5, Mockito, Spring Security Test |

---

## 2. Especificación del servicio

### Endpoint

```
GET /api/v1/students/{studentCode}/academic-record
```

| Parámetro | Ubicación | Obligatorio | Descripción |
|---|---|---|---|
| `studentCode` | path | sí | Código institucional del estudiante (p. ej. `A00123456`). |
| `term` | query | no | Periodo a consultar con formato `AAAA-1` / `AAAA-2`. Si se omite, se usa el **periodo vigente**. |

### Autenticación

Cabecera `Authorization: Bearer <JWT>`. El token lo emite la plataforma de identidad
(OIDC). Claims usados:

| Claim | Uso |
|---|---|
| `sub` | Identificador del usuario. Para un asesor, se cruza con `advisor_assignment`. |
| `roles` | Lista con `STUDENT` y/o `ADVISOR`. |
| `student_id` | Solo en tokens de estudiante: su propio `studentCode`. |

### Autorización (a nivel de recurso)

- **Estudiante** (`STUDENT`): solo puede consultar su propia información
  (`studentCode` del path == `student_id` del token).
- **Asesor** (`ADVISOR`): solo puede consultar a los estudiantes que tiene **asignados**
  (tabla `advisor_assignment`).
- Cualquier otro caso → `403`.

La regla se evalúa **en el backend con los datos del token**; nunca se confía en un
identificador enviado por el cliente.

### Respuesta `200 OK`

```json
{
  "student": { "code": "A00123456", "fullName": "Laura Gómez", "program": "Ingeniería de Sistemas" },
  "term":    { "code": "2025-2", "name": "Segundo semestre 2025" },
  "enrolledCourses": [
    {
      "courseCode": "IS-101",
      "courseName": "Introducción a la Ingeniería de Software",
      "credits": 3,
      "group": "01",
      "professor": "Ana Torres",
      "status": "ENROLLED",
      "grade": 4.30
    },
    {
      "courseCode": "IS-205",
      "courseName": "Estructuras de Datos",
      "credits": 4,
      "group": "01",
      "professor": "Pedro Salas",
      "status": "ENROLLED",
      "grade": null
    }
  ]
}
```

- `grade` es `null` cuando la materia aún no tiene calificación registrada.
- Las materias **canceladas** (`WITHDRAWN`) no se incluyen.

### Errores (RFC 9457 — `application/problem+json`)

| Código | Cuándo |
|---|---|
| `400` | `studentCode` o `term` con formato inválido. |
| `401` | Falta el token o no es válido. |
| `403` | El usuario no puede ver a ese estudiante. |
| `404` | El estudiante o el periodo no existen. |

---

## 3. Modelo de datos

```
program ──1:N──┐
               ▼
academic_term       student ──N:1── program
     │  1:N            │
     ▼                 │ 1:N
course_offering ──1:N──┴──► enrollment ──N:1── course_offering
     ▲                            │
     │ N:1                        │  (status, enrolled_at, grade)
   course                         │
                                  ▼
                        advisor_assignment ──N:1── student
                        (advisor_subject, active)
```

| Tabla | Propósito |
|---|---|
| `program` | Programa académico. |
| `academic_term` | Periodo (semestre); `is_current` marca el vigente. |
| `student` | Estudiante (proyección local del ERP — supuesto S6). |
| `course` | Materia del catálogo. |
| `course_offering` | Oferta de una materia en un periodo (grupo + profesor). |
| `enrollment` | Matrícula de un estudiante en una oferta, con `status` y `grade`. |
| `advisor_assignment` | Relación asesor → estudiante asignado (base de la autorización). |

Esquema en [`src/main/resources/db/migration/V1__academic_schema.sql`](src/main/resources/db/migration/V1__academic_schema.sql).
Datos de ejemplo en [`src/main/resources/db/seed/V900__seed_sample_data.sql`](src/main/resources/db/seed/V900__seed_sample_data.sql).

---

## 4. Cómo ejecutar

### Por defecto — H2 en memoria, sin instalar nada

```bash
./gradlew bootRun
```

Arranca en `http://localhost:8080` con el esquema y los datos de ejemplo ya cargados.
No requiere base de datos externa ni proveedor OIDC. Consola H2 en `/h2-console`
(JDBC URL `jdbc:h2:mem:vista360`, usuario `sa`, sin contraseña).

### Pruebas

```bash
./gradlew test
```

### Perfil `prod` — PostgreSQL + plataforma de identidad

```bash
java -jar build/libs/vista-360-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Variables de entorno:

| Variable | Descripción |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL. |
| `OIDC_ISSUER_URI` | Emisor OIDC. El perfil `prod` valida el JWT contra sus llaves públicas (no usa secreto simétrico). |

En `prod` **no** se cargan los datos de ejemplo (`spring.flyway.locations` solo incluye `db/migration`).

---

## 5. Probar el endpoint

Por defecto los JWT se validan con un secreto simétrico (HS256) **solo apto para desarrollo**
(`app.security.jwt.strategy: hmac`). Tokens de ejemplo ya firmados (rol estudiante y rol
asesor, vigencia ~10 años):

<details>
<summary>STUDENT_TOKEN (estudiante A00123456)</summary>

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdHVkZW50LUEwMDEyMzQ1NiIsInJvbGVzIjpbIlNUVURFTlQiXSwic3R1ZGVudF9pZCI6IkEwMDEyMzQ1NiIsImlhdCI6MTc4Nzg0MTQxNSwiZXhwIjoyMTAzMjAxNDE1fQ.HcRv7Q3F5T5lk1KCZxtn7g2GhsWH9kmIoxEkxSGh_8c
```
</details>

<details>
<summary>ADVISOR_TOKEN (asesor advisor-001, asignado a A00123456 y A00111222)</summary>

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZHZpc29yLTAwMSIsInJvbGVzIjpbIkFEVklTT1IiXSwiaWF0IjoxNzg3ODQxNDE1LCJleHAiOjIxMDMyMDE0MTV9.UHlz4vbSqjErwwkxsIhm16zH9DFtY1R5NJWj5wsFWpQ
```
</details>

```bash
TOKEN="<STUDENT_TOKEN>"

# La información propia del estudiante
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/students/A00123456/academic-record

# Un periodo anterior
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/students/A00123456/academic-record?term=2025-1"

# Intentar ver a otro estudiante -> 403
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/students/A00987654/academic-record
```

Generar tokens nuevos (el secreto está en `application.yml`, clave `app.security.jwt.hmac-secret`):

```python
import hmac, hashlib, base64, json, time
b64 = lambda b: base64.urlsafe_b64encode(b).rstrip(b'=')
sec = "clave-solo-para-desarrollo-local-no-usar-en-produccion-0123456789"
payload = {"sub": "student-A00123456", "roles": ["STUDENT"], "student_id": "A00123456",
           "iat": int(time.time()), "exp": int(time.time()) + 3600}
h = b64(b'{"alg":"HS256","typ":"JWT"}')
p = b64(json.dumps(payload, separators=(",", ":")).encode())
s = b64(hmac.new(sec.encode(), h + b"." + p, hashlib.sha256).digest())
print((h + b"." + p + b"." + s).decode())
```

---

## 6. Estructura del proyecto

```
src/main/java/com/test_icesi/vista_360
├── config
│   ├── SecurityConfig            # cadena de filtros: /api/** requiere JWT
│   └── HmacJwtConfig             # decodificador HS256 para local/pruebas
└── academicrecord
    ├── api
    │   ├── AcademicRecordController      # GET .../academic-record
    │   ├── ApiExceptionHandler           # errores -> ProblemDetail
    │   └── dto/                          # records de respuesta
    ├── domain                            # entidades JPA + repositorios
    ├── service
    │   ├── AcademicRecordService         # lógica de consulta
    │   └── StudentNotFoundException / TermNotFoundException
    └── security
        ├── AccessControlService          # autorización por recurso
        └── AdvisorAssignment(+Repository)
```

---

## 7. Decisiones de diseño y buenas prácticas

- **Servicio autónomo con su propia BD.** Representa la *proyección académica* que en
  producción se alimentaría del ERP por eventos/CDC (supuesto S6). Así el servicio es
  desplegable y probable sin depender de un sistema externo.
- **`studentCode` como identificador público**, no el `id` interno: estable y no expone
  la clave primaria.
- **Resource Server + autorización a nivel de recurso** separada en `AccessControlService`,
  no dispersa en el controlador.
- **Sin interfaz sobre las clases `@Service` (decisión, no omisión).** Una interfaz con una
  única implementación y la misma firma solo añade indirección (más archivos, navegación
  peor) sin ganar nada: Spring proxya clases concretas con CGLIB y Mockito mockea clases.
  El límite donde la implementación sí puede variar —el acceso a datos— **ya está detrás de
  interfaces**: los repositorios de Spring Data (`StudentRepository`, `EnrollmentRepository`,
  …). Ese es el *puerto* real: si la proyección académica pasara a alimentarse del ERP
  (supuesto S6), se sustituye el adaptador de repositorio, no el servicio. Se introduciría
  una interfaz de servicio el día que haya una segunda implementación o un contrato
  publicado a otros módulos.
- **Flyway** para el esquema; migraciones separadas de los datos semilla (los datos de
  ejemplo no se cargan en el perfil `prod`).
- **DTOs `record`** desacoplados de las entidades; sin exponer entidades JPA en la API.
- **Errores RFC 9457** homogéneos vía `@RestControllerAdvice`.
- **Consulta única con `join fetch`** para evitar el problema N+1.
- **`open-in-view: false`** y `@Transactional(readOnly = true)` en la consulta.
- **Perfiles**: por defecto (H2 + JWT simétrico, arranca sin dependencias) vs `prod`
  (PostgreSQL + OIDC real).
- **Pruebas por capa**: unitarias del servicio y de la autorización (Mockito),
  de repositorio (`@DataJpaTest` sobre H2 + Flyway) y de API (`@WebMvcTest` con seguridad).
- **Versionado**: commits pequeños con Conventional Commits.

---

## 8. Estado de la entrega

### Implementado

- [x] Especificación del servicio (contrato REST, request/response, errores).
- [x] Modelo de datos con migraciones Flyway y datos de ejemplo.
- [x] Endpoint `GET /api/v1/students/{studentCode}/academic-record` con filtro por periodo.
- [x] Autenticación por JWT (OAuth2 Resource Server) y autorización por recurso
      (estudiante dueño / asesor asignado).
- [x] Manejo de errores RFC 9457.
- [x] Pruebas unitarias, de repositorio y de API (17 pruebas).
- [x] Health/metrics con Actuator.

### Qué tendría a continuación (fuera de las 3 horas)

- **Notas parciales**: tabla `grade_item` (peso + calificación) y cálculo de la nota
  vigente cuando no hay nota definitiva.
- **Sincronización desde el ERP**: consumidor de eventos `EnrollmentChanged` /
  `GradeRegistered` que mantiene la proyección al día (hoy los datos son semilla).
- **Auditoría de acceso** (Parte 4.B): registrar cada consulta de un estudiante en un log
  inmutable.
- **Contenedores**: `Dockerfile` + `docker-compose` con PostgreSQL.
- **OpenAPI** (`springdoc`) para publicar el contrato.
- **Observabilidad**: trazado distribuido con OpenTelemetry y `correlation-id` (Parte 4.A).
- **Paginación/caché** si el volumen por estudiante creciera.
- Integración real con la plataforma de identidad (hoy validada con los tests y con el
  perfil por defecto de JWT simétrico).

---

## 9. Uso de IA

| Herramienta | Dónde | Propósito |
|---|---|---|
| Claude (Claude Code) | Análisis del enunciado, redacción de `docs/` y de este README, andamiaje del servicio y de las pruebas | Acelerar análisis y *boilerplate*. Cada decisión de arquitectura, el modelo de datos, el contrato y las reglas de autorización fueron revisados y validados manualmente (build + pruebas + `curl` de extremo a extremo). |
