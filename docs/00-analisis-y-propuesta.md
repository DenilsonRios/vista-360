# Vista 360° del Estudiante — Análisis de la prueba y propuesta de solución

> Documento de trabajo (Fase 1). Se refina en las siguientes fases junto con los diagramas
> y las respuestas a las Partes 3 y 4.

---

## 1. Desglose de lo que piden

### 1.1 Marco de la prueba

| Aspecto | Detalle |
|---|---|
| Rol evaluado | Ingeniero de Arquitectura e Innovación |
| Duración | 3 horas |
| Qué se evalúa | **Razonamiento y solidez de las decisiones**, no la tecnología elegida. Justificar pesa más que nombrar. En la implementación, buenas prácticas. |
| IA | Permitida. Hay que declarar: qué herramienta, en qué partes y con qué propósito. |
| Zonas grises | El caso es abierto a propósito. Ante falta de información: **formular un supuesto razonable, declararlo explícitamente y justificarlo.** |

### 1.2 El caso

**Vista 360°** es una plataforma **nueva** que centraliza la información relevante de cada
estudiante. **No reemplaza** ningún sistema: se apoya en el ecosistema actual y añade una
**capa propia** de información y funcionalidad.

**Usuarios:**

- **Estudiante** → consulta su propia información.
- **Equipo de acompañamiento** → consulta los estudiantes a su cargo, registra reportes de
  acompañamiento y alertas, e identifica situaciones para intervención temprana.

**Ecosistema actual (del diagrama de la prueba):**

| Sistema | Rol | Ubicación / acceso |
|---|---|---|
| **ERP institucional** | Datos maestros, académicos y financieros. **Fuente de verdad.** | On-premise. Algunas APIs y acceso a BD. |
| **Plataforma LMS** | Actividad del estudiante en el campus virtual. | Cloud. Acceso por API. |
| **Plataforma de identidad** | Autenticación de estudiantes y personal. | Estándares abiertos de identidad (OIDC/SAML). |
| **Plataforma de integración** | Mediación, orquestación y mensajería entre aplicaciones. | ESB / iPaaS / broker. |
| **Data warehouse** | Repositorio analítico del ecosistema; base de los modelos de analítica. | — |

**Lo que Vista 360° debe lograr:**

1. El estudiante ve en un solo lugar: información personal, académica y financiera + su
   actividad en el campus virtual.
2. El equipo de acompañamiento consulta a sus estudiantes y **registra reportes de
   acompañamiento, alertas y solicitudes. Estos registros son NUEVOS: no existen hoy en
   ningún sistema.**
3. La información de acompañamiento **se persiste** para consulta y gestión.
4. La información del ecosistema debe **poder alimentar el data warehouse** sobre el que se
   construirán los modelos de analítica.

### 1.3 Entregables exigidos

| Parte | Entregable |
|---|---|
| **1 — Diseño** | Diagrama de arquitectura + documento breve con decisiones y supuestos. Debe dejar claro: (a) para **cada dato**, de dónde se obtiene y por qué; (b) cómo se **comunican** los componentes. |
| **2 — Servicio** | Especificación + diseño de BD + implementación (lenguaje/framework libre) + **repo público de GitHub** + README. Se evalúa también el **versionado**. |
| **3 — Seguridad y comunicación** | Respuestas argumentadas: 3.1 AuthN/AuthZ; 3.2 Escenario A (consulta financiera inmediata) y B (cambio de condición académica → procesos + DWH). |
| **4 — Operación y calidad** | Respuestas argumentadas: A (fallo intermitente de carga académica); B (reclamo de acceso/alteración indebida de información). |

---

## 2. Supuestos declarados

| # | Supuesto | Justificación |
|---|---|---|
| S1 | La **Plataforma de Integración** expone de forma mediada las APIs del ERP y del LMS, y además ofrece (o tiene junto a ella) un **broker de eventos/mensajería** (tipo Kafka/colas). | El diagrama la describe como "mediación, orquestación y mensajería". Nos apoyamos en ella en vez de integrarnos punto a punto. |
| S2 | El ERP puede **emitir eventos** de cambios relevantes (o se le puede aplicar CDC sobre su BD). Si no fuera posible, hay un *fallback* de *polling*/batch. | El ERP "tiene algunas APIs y acceso a la BD". Necesario para la reacción temprana (3.2.B). |
| S3 | La Plataforma de Identidad soporta **OIDC** (Authorization Code + PKCE) para usuarios y **client credentials** para servicios. | Se describe con "estándares abiertos de identidad". No reinventamos el IdP. |
| S4 | Vista 360° es **solo lectura** sobre datos del ERP y del LMS. La única información de la que es *fuente de verdad* es la de **acompañamiento** (reportes, alertas, solicitudes, asignaciones). | El enunciado dice que no reemplaza sistemas y que solo esos registros son nuevos. |
| S5 | La **asignación asesor ↔ estudiante** nace en RRHH/ERP y se sincroniza; mientras tanto se administra dentro de Vista 360°. | Es la base de la autorización del equipo de acompañamiento y no está definida en el caso. |
| S6 | Para la **Parte 2**, al no haber acceso real al ERP, el servicio usa su **propia base de datos con datos de ejemplo**, que representa la *proyección local* que en producción se alimentaría del ERP (vía eventos/CDC). | Permite entregar un servicio funcional y versionable sin depender de un sistema externo inexistente en la prueba. |
| S7 | Volúmenes moderados (universidad: decenas de miles de estudiantes, picos en matrícula). No se requiere arquitectura hiperescalable; **PostgreSQL** es suficiente. | Dimensiona las decisiones (una BD relacional, no *sharding*). |
| S8 | El frontend es una **SPA web** (y potencialmente app móvil a futuro) que consume una API. | Justifica el patrón BFF y la agregación en backend. |
| S9 | El estado financiero y los datos académicos oficiales **se leen del ERP**; Vista 360° no los "recalcula". | El ERP es fuente de verdad. |

---

## 3. Propuesta de arquitectura (resumen)

### 3.1 Principio rector

**Agregación en el backend, no en el frontend.** Vista 360° expone un **BFF** que compone la
vista del estudiante a partir de: (a) datos de referencia del **ERP** y del **LMS** obtenidos
a través de la **Plataforma de Integración**, y (b) datos propios del servicio de
**Acompañamiento**. El frontend nunca habla con el ERP/LMS directamente.

### 3.2 Componentes propios de Vista 360°

| Componente | Responsabilidad | Persistencia |
|---|---|---|
| **API Gateway / BFF** | Punto único de entrada del frontend. Valida el token OIDC, aplica *rate limiting*, propaga contexto de trazado, orquesta llamadas a los servicios de dominio. | — |
| **Servicio Perfil 360 (agregador)** | Compone la vista: personal/académico/financiero (ERP) + actividad (LMS) + resumen de acompañamiento. Aplica resiliencia (timeouts, *circuit breaker*, degradación parcial). | Cache de corta duración (opcional). |
| **Servicio Acompañamiento** | *Fuente de verdad* de reportes, alertas, solicitudes y asignaciones asesor↔estudiante. Motor de reglas de alerta temprana. | **BD Vista 360° (PostgreSQL).** |
| **Servicio Registro Académico** (Parte 2) | Dado un `studentId`, devuelve materias matriculadas y notas del periodo actual. En la prueba se implementa como servicio autónomo con BD propia. | **BD propia (proyección académica).** |
| **Publicador de eventos (Outbox)** | Publica de forma confiable los eventos de dominio de Vista 360° hacia el broker. | Tabla `outbox` en la BD Vista 360°. |

### 3.3 De dónde sale cada dato

| Dato | Origen (fuente de verdad) | Cómo lo obtiene Vista 360° | Por qué así |
|---|---|---|---|
| Identidad / inicio de sesión | Plataforma de identidad | OIDC (Authorization Code + PKCE) | Es la fuente y usa estándares abiertos; no se replican credenciales. |
| Datos personales del estudiante | ERP (maestros) | API mediada por la Plataforma de Integración, *on-demand* + cache corta | Fuente de verdad; cambian poco; tolera cache. |
| Datos académicos oficiales (matrícula, notas, condición) | ERP | API *on-demand*; además **proyección local** alimentada por eventos/CDC para analítica y para el servicio de la Parte 2 | Fuente de verdad; lectura frecuente; la proyección desacopla y habilita el DWH. |
| Estado financiero / estado de cuenta | ERP | **API síncrona *on-demand*, sin persistir** (o cache de segundos) | Dato **volátil y sensible**; el usuario necesita el valor exacto "ahora" (Escenario 3.2.A). |
| Actividad en el campus virtual | LMS (cloud) | API del LMS mediada; cache con TTL medio | Sistema externo; tolera algo de latencia de frescura. |
| Reportes / alertas / solicitudes de acompañamiento | **Vista 360° (propio)** | BD propia | No existen hoy en ningún sistema; son nuevos. |
| Asignación asesor ↔ estudiante | **Vista 360° (propio)**, semilla desde RRHH/ERP | BD propia | Es la base de la autorización del equipo de acompañamiento. |
| Alimentación del Data Warehouse | Todos los sistemas | **Eventos de dominio + CDC → tópicos → sink al DWH (ELT)** | Desacopla; el DWH no consulta las bases transaccionales ni al revés. |

### 3.4 Cómo se comunican los componentes

| Enlace | Estilo | Mecanismo / seguridad |
|---|---|---|
| Frontend → BFF | Síncrono | REST/HTTPS + JWT de OIDC (token de usuario). |
| BFF → servicios de dominio | Síncrono | REST interno (o gRPC), **mTLS** + token de servicio (OAuth2 *client credentials*), red privada. |
| Vista 360° → ERP / LMS (lecturas puntuales) | Síncrono | API mediada por la **Plataforma de Integración**. Timeouts + *circuit breaker*. |
| ERP → Vista 360° (cambios relevantes) | Asíncrono | Evento (`AcademicStandingChanged`, …) publicado en el broker; Vista 360° se suscribe. |
| Vista 360° → otros procesos / DWH | Asíncrono | **Transactional Outbox** → broker → consumidores + *sink* al DWH. Entrega *at-least-once*, eventos idempotentes con `eventId`, partición por `studentId`. |

### 3.5 Seguridad (adelanto de la Parte 3.1)

- **AuthN de usuarios:** OIDC contra la Plataforma de identidad. *Access token* JWT de vida
  corta + *refresh token*.
- **AuthZ:**
  - *Estudiante* → el backend **fuerza** `token.studentId == recurso.studentId`. Nunca se
    confía en el identificador que envía el frontend.
  - *Acompañamiento* → rol `advisor` **+** verificación de la **relación de asignación**
    (`advisor_id` tiene asignado a `student_id`) → autorización a nivel de recurso
    (*relationship-based* / ABAC). Preferible un *policy decision point* (OPA/Cedar); como
    mínimo `@PreAuthorize` + un servicio de autorización central.
- **Servicio ↔ servicio:** OAuth2 *client credentials* + mTLS + mínimo privilegio por *scope*.
- **Datos sensibles:** TLS en tránsito, cifrado en reposo, minimización de PII, el estado
  financiero no se cachea (o TTL mínimo).
- **Auditoría:** todo acceso a datos de un estudiante se registra (quién, qué, cuándo,
  resultado) en un log **append-only**.

---

## 4. Parte 2 — Servicio de registro académico (diseño previo)

### 4.1 Especificación (borrador)

```
GET /api/v1/students/{studentId}/academic-record?term={termCode}
Authorization: Bearer <JWT>
```

- **Recibe:** `studentId` (path, obligatorio). `term` (query, opcional; por defecto el
  periodo académico vigente).
- **Devuelve (200):**

```jsonc
{
  "student": { "id": "...", "fullName": "...", "program": "..." },
  "term": { "code": "2025-2", "name": "Segundo semestre 2025" },
  "enrolledCourses": [
    {
      "courseCode": "IS-101",
      "courseName": "Introducción a la Ingeniería de Software",
      "credits": 3,
      "group": "A",
      "status": "ENROLLED",
      "currentGrade": 4.2          // null si aún no tiene nota
    }
  ]
}
```

- **Errores:** `401` sin token / token inválido · `403` no es su información o el asesor no
  tiene asignado al estudiante · `404` estudiante inexistente · `200` con
  `enrolledCourses: []` si no hay matrícula activa.

### 4.2 Modelo de datos base (PostgreSQL)

| Tabla | Campos clave |
|---|---|
| `student` | `id`, `document`, `first_name`, `last_name`, `program_id`, `status`, `email` |
| `program` | `id`, `name`, `faculty` |
| `academic_term` | `id`, `code`, `name`, `start_date`, `end_date`, `is_current` |
| `course` | `id`, `code`, `name`, `credits` |
| `course_offering` | `id`, `course_id`, `term_id`, `group_code`, `professor_name` |
| `enrollment` | `id`, `student_id`, `course_offering_id`, `status`, `enrolled_at`, `final_grade` (nullable); UNIQUE `(student_id, course_offering_id)` |
| `grade_item` *(opcional)* | `id`, `enrollment_id`, `name`, `weight`, `score`, `graded_at` — para notas parciales |

Índices: `enrollment(student_id)`, `course_offering(term_id)`, `academic_term(is_current)`.

### 4.3 Implementación prevista

- **Stack:** Spring Boot 4.1.1, Java 21, Spring Web MVC, Spring Data JPA, PostgreSQL
  (H2 para pruebas), **Flyway** para migraciones y datos semilla.
- **Capas:** `controller` → `service` → `repository`; DTOs de salida separados de las
  entidades; `@RestControllerAdvice` para el manejo global de errores; *bean validation*.
- **Pruebas:** unit del `service`; `@WebMvcTest` del controller; `@DataJpaTest` de los
  repositorios; opcional integración con Testcontainers.
- **Seguridad:** filtro JWT que valida el token y aplica la regla
  `token.studentId == path.studentId` (y la relación de asignación para asesores).
- **Versionado:** *Conventional Commits*, ramas `feature/*`, historia limpia.

### 4.4 Ajustes pendientes al `build.gradle` actual

Faltan por añadir en Fase 2: driver de PostgreSQL + H2, `spring-boot-starter-validation`,
`spring-boot-starter-actuator`, `spring-boot-starter-security` / `oauth2-resource-server`,
Flyway, y (para pruebas) Testcontainers. Además, `spring-cloud-starter-config` intentará
contactar un Config Server al arrancar: para desarrollo local se desactivará con
`spring.cloud.config.enabled=false` o `spring.config.import=optional:configserver:`.

---

## 5. Adelanto de las Partes 3.2 y 4

| Escenario | Enfoque propuesto (se argumenta en su fase) |
|---|---|
| **3.2.A** — estado financiero inmediato | Consulta **síncrona *on-demand*** BFF → Perfil 360 → API financiera del ERP. **No se persiste** el saldo. Timeout corto + *circuit breaker*; si el ERP no responde, respuesta **parcial** en vez de fallar toda la vista. Exactitud > frescura de cache; *pull* síncrono es lo correcto para un dato que se lee puntualmente. |
| **3.2.B** — cambio de condición académica | **Event-driven.** El ERP emite `AcademicStandingChanged` → broker. Acompañamiento se suscribe y dispara la alerta/intervención temprana. El mismo evento fluye al **DWH** vía *sink*/conector. *At-least-once* + idempotencia + orden por `studentId`. Outbox evita pérdida de eventos. Justificación: múltiples consumidores desacoplados y el DWH no se acopla a las OLTP. |
| **4.A** — fallo intermitente de carga académica | **Afrontar:** runbook, dashboards de la dependencia ERP (latencia p99, error rate, pool de conexiones), correlación por `traceId`, búsqueda de patrón (programa, hora pico, nodo). Hipótesis: *timeouts*/agotamiento de conexiones/*rate limit* del ERP. **Previsto desde diseño:** *tracing* distribuido (OpenTelemetry) con *correlation IDs* end-to-end, logs estructurados, métricas RED por dependencia, *health checks* y *synthetic monitoring*, *circuit breaker* + reintentos con *backoff* + *fallback* a la última proyección conocida marcada como "dato no actualizado", *timeouts* explícitos, alertas por *error-rate*, degradación parcial. |
| **4.B** — reclamo de acceso/alteración indebida | **Previsto desde diseño:** log de auditoría de acceso **inmutable** (WORM o *hash-chain*/firma) con actor, rol, recurso, acción, timestamp, IP, resultado y propósito; historial de cambios (versionado/*event sourcing*) de los datos propios; integridad por hash encadenado; autorización a nivel de fila verificable y probada; retención y no repudio; capacidad de emitir un reporte "quién consultó la información de este estudiante entre X e Y"; segregación de funciones y revisión de accesos privilegiados. |

---

## 6. Uso de IA (declaración, se completa al final)

| Herramienta | Parte(s) | Propósito |
|---|---|---|
| Claude (Claude Code) | Análisis del enunciado, redacción de este documento, apoyo en el diseño y en la implementación del servicio | Acelerar el análisis y la redacción; el candidato revisa y decide cada elección. |

---

## 7. Plan de trabajo por fases

1. **Fase 1 — Análisis y propuesta** *(este documento).*
2. **Fase 2 — Desarrollo** del servicio de la Parte 2 (spec → migraciones → dominio → API →
   seguridad → pruebas → README).
3. **Fase 3 — Diagramas** (contexto/contenedores C4, flujo de datos, secuencias de 3.2.A y
   3.2.B).
4. **Fase 4 — Respuestas analíticas** a las Partes 3 y 4, con decisiones y supuestos.
