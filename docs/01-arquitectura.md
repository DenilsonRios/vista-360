# Vista 360° — Arquitectura de la solución (Parte 1)

Documento breve con el diagrama de arquitectura, las decisiones más importantes y los
supuestos declarados. El análisis completo del caso está en
[`00-analisis-y-propuesta.md`](00-analisis-y-propuesta.md).

---

## 1. Alcance y principio rector

Vista 360° es una **capa propia** sobre el ecosistema existente. No reemplaza sistemas:
lee datos de los sistemas fuente y **añade** la información y la funcionalidad de
acompañamiento, que hoy no existe en ningún lado.

**Principio rector: la vista se compone en el backend, no en el frontend.** Un
*Backend for Frontend* (BFF) orquesta y agrega; el frontend nunca habla con el ERP ni con
el LMS.

---

## 2. Diagrama de arquitectura

```mermaid
flowchart TB
    EST([Estudiante])
    ACO([Equipo de acompañamiento])
    SPA[Frontend SPA]

    subgraph v360["Vista 360° — plataforma nueva"]
      direction TB
      BFF["API Gateway / BFF"]
      AGG["Servicio Perfil 360<br/>(agregador + resiliencia)"]
      ACC["Servicio Acompañamiento<br/>(fuente de verdad de lo nuevo)"]
      RA["Servicio Registro Académico<br/>· Parte 2 · IMPLEMENTADO"]
      DB[("BD Vista 360°<br/>PostgreSQL")]
      OBX[["Outbox → publicador de eventos"]]
    end

    subgraph eco["Ecosistema existente"]
      direction TB
      IDP["Plataforma de Identidad<br/>(OIDC)"]
      INT["Plataforma de Integración<br/>(mediación + broker de eventos)"]
      ERP[("ERP institucional<br/>on-premise · fuente de verdad")]
      LMS[("LMS · cloud")]
      DWH[("Data Warehouse")]
    end

    EST --> SPA
    ACO --> SPA
    SPA -->|"HTTPS + JWT"| BFF
    SPA -.->|"OIDC Auth Code + PKCE"| IDP
    BFF -->|"REST interno · mTLS + client credentials"| AGG
    BFF -->|"REST interno · mTLS"| ACC
    BFF -->|"REST interno · mTLS"| RA
    AGG -->|"API mediada (síncrono)"| INT
    INT --> ERP
    INT --> LMS
    ACC --> DB
    RA --> DB
    ERP -. "evento: AcademicStandingChanged" .-> INT
    INT -. "suscripción (alerta temprana)" .-> ACC
    OBX -. "eventos de dominio" .-> INT
    INT -. "sink / ELT" .-> DWH
    ERP -. "CDC / eventos → proyección académica" .-> DB
```

**Leyenda:** línea **sólida** = comunicación **síncrona** (petición/respuesta);
línea **punteada** = comunicación **asíncrona** (eventos / CDC).

---

## 3. Para cada dato: de dónde sale y por qué

| Dato | Fuente de verdad | Cómo lo obtiene Vista 360° | Por qué así |
|---|---|---|---|
| Identidad / inicio de sesión | Plataforma de Identidad | OIDC (Authorization Code + PKCE) | Es la fuente y usa estándares abiertos; no se replican credenciales. |
| Datos personales | ERP | API mediada por la Plataforma de Integración, *on-demand* + cache corta | Fuente de verdad; cambian poco; toleran cache. |
| Datos académicos oficiales (matrícula, notas, condición) | ERP | API *on-demand* **+ proyección local** alimentada por eventos/CDC | Lectura frecuente; la proyección desacopla y habilita analítica y el servicio de la Parte 2. |
| **Estado financiero / estado de cuenta** | ERP | **API síncrona *on-demand*, sin persistir** (cache de segundos como mucho) | Dato **volátil y sensible**; el usuario necesita el valor exacto "ahora" (Escenario 3.2.A). |
| Actividad en el campus virtual | LMS (cloud) | API del LMS mediada; cache con TTL medio | Sistema externo; tolera algo de latencia de frescura. |
| Reportes / alertas / solicitudes de acompañamiento | **Vista 360° (propio)** | BD propia | No existen hoy en ningún sistema; son nuevos. |
| Asignación asesor ↔ estudiante | **Vista 360° (propio)**, semilla desde RRHH/ERP | BD propia | Es la base de la autorización del equipo de acompañamiento. |
| Alimentación del Data Warehouse | Todos los sistemas | **Eventos de dominio + CDC → tópicos → sink al DWH (ELT)** | Desacopla; el DWH no consulta las bases transaccionales ni al revés. |

---

## 4. Cómo se comunican los componentes

| Enlace | Estilo | Mecanismo y seguridad |
|---|---|---|
| Frontend → BFF | Síncrono | REST/HTTPS + JWT de OIDC (token de usuario). |
| BFF → servicios de dominio | Síncrono | REST interno (o gRPC), **mTLS** + token de servicio (OAuth2 *client credentials*), red privada. |
| Vista 360° → ERP / LMS (lecturas puntuales) | Síncrono | API mediada por la Plataforma de Integración. Timeout + *circuit breaker* + degradación parcial. |
| ERP → Vista 360° (cambios relevantes) | Asíncrono | Evento publicado en el broker; Vista 360° se suscribe. |
| Vista 360° → otros procesos / DWH | Asíncrono | **Transactional Outbox** → broker → consumidores + *sink* al DWH. Entrega *at-least-once*, eventos idempotentes por `eventId`, partición por `studentId`. |

### 4.1 Escenario 3.2.A — el estudiante necesita ver su estado financiero de inmediato

```mermaid
sequenceDiagram
    actor E as Estudiante
    participant F as Frontend
    participant B as BFF
    participant P as Perfil 360
    participant I as Plataforma de Integración
    participant ERP as ERP (financiero)

    E->>F: Abre Vista 360°
    F->>B: GET /students/:id/overview (JWT)
    B->>B: Valida token y autoriza (es su propia info)
    B->>P: Componer la vista del estudiante
    P->>I: GET estado de cuenta  (timeout 2 s, circuit breaker)
    I->>ERP: Consulta saldo
    ERP-->>I: Saldo actual
    I-->>P: Saldo actual
    Note over P: No se persiste el saldo (dato volátil y sensible)
    P-->>B: Vista compuesta (incluye estado financiero)
    B-->>F: 200 overview
    Note over B,F: Si el ERP no responde → respuesta parcial:<br/>"estado financiero no disponible ahora", el resto de la vista carga
```

**Fundamento:** es un dato *read-your-own*, de baja cardinalidad por petición, que exige
**exactitud sobre frescura de cache**. Un *pull* síncrono contra la fuente es lo correcto;
no se justifica una arquitectura de eventos para un dato que se lee de forma puntual. La
resiliencia (timeout corto, *circuit breaker*, degradación parcial) evita que una demora
del ERP tumbe toda la Vista 360°.

### 4.2 Escenario 3.2.B — cambia la condición académica de un estudiante

```mermaid
sequenceDiagram
    participant ERP as ERP
    participant I as Integración / Broker
    participant ACC as Servicio Acompañamiento
    participant DB as BD Vista 360°
    participant OTR as Otros procesos
    participant DWH as Data Warehouse

    ERP->>I: publica AcademicStandingChanged<br/>(eventId, studentId, nuevaCondición)
    I->>ACC: entrega evento (at-least-once · orden por studentId)
    ACC->>ACC: Evalúa regla de alerta temprana
    ACC->>DB: Persiste alerta / caso de intervención
    ACC->>I: publica EarlyAlertRaised (vía Outbox)
    I->>OTR: entrega (becas, bienestar, …)
    I->>DWH: sink / conector → tabla de hechos
    Note over ACC,DWH: Idempotencia por eventId · el DWH no se acopla a las BD transaccionales
```

**Fundamento:** hay **varios consumidores desacoplados** (acompañamiento, otros procesos,
DWH) y la plataforma debe **reaccionar temprano**. Un evento de dominio publicado una vez y
consumido por quien lo necesite es lo correcto. El **Outbox** garantiza que el evento no se
pierda aunque el broker esté caído; la **idempotencia** y el **orden por `studentId`**
hacen segura la entrega *at-least-once*. El DWH se alimenta por el mismo flujo de eventos
(ELT), nunca consultando las bases transaccionales.

---

## 5. Decisiones más importantes

| Decisión | Fundamento |
|---|---|
| **BFF que agrega en el backend** | Una sola llamada del frontend; la lógica de composición, resiliencia y autorización vive en un lugar; el frontend no conoce el ecosistema. |
| **Servicio de Acompañamiento como fuente de verdad de lo nuevo, con BD propia (PostgreSQL)** | Es la única información que Vista 360° posee; el resto lo lee de los sistemas fuente (supuesto S4). |
| **Proyección académica local alimentada por eventos/CDC** | Desacopla a Vista 360° de la disponibilidad del ERP para lecturas frecuentes y habilita el DWH. El servicio de la Parte 2 se construye sobre esta proyección. |
| **Estado financiero siempre en vivo desde el ERP, sin persistir** | Dato volátil y sensible; la exactitud manda. |
| **Síncrono para "leer ahora", asíncrono para "propagar cambios"** | Cada estilo donde corresponde; ni todo REST ni todo eventos. |
| **Transactional Outbox para publicar eventos** | Publicación confiable sin *dual write* entre la BD y el broker. |
| **DWH alimentado por eventos/CDC, no por consultas** | El almacén analítico no se acopla a las bases transaccionales. |
| **Identidad delegada a la plataforma OIDC** | Estándar abierto; Vista 360° es *resource server*, no IdP. |
| **Autorización a nivel de recurso** (estudiante dueño / asesor asignado) evaluada en el backend con el token | El frontend no es de fiar; la relación asesor↔estudiante se verifica contra datos propios. |

---

## 6. Supuestos declarados

Resumen (detalle y justificación en [`00-analisis-y-propuesta.md`](00-analisis-y-propuesta.md#2-supuestos-declarados)):

| # | Supuesto |
|---|---|
| S1 | La Plataforma de Integración expone las APIs mediadas del ERP y del LMS y ofrece un broker de eventos/mensajería. |
| S2 | El ERP puede emitir eventos o se le puede aplicar CDC; si no, hay *fallback* de *polling*/batch. |
| S3 | La Plataforma de Identidad soporta OIDC (Auth Code + PKCE) para usuarios y *client credentials* para servicios. |
| S4 | Vista 360° es **solo lectura** sobre ERP y LMS; solo es fuente de verdad de la información de acompañamiento. |
| S5 | La asignación asesor↔estudiante nace en RRHH/ERP y se sincroniza; mientras tanto se administra en Vista 360°. |
| S6 | Para la Parte 2, el servicio usa su propia BD con datos de ejemplo, que representa la proyección alimentada del ERP. |
| S7 | Volúmenes moderados (decenas de miles de estudiantes) → PostgreSQL es suficiente. |
| S8 | El frontend es una SPA web (posible app móvil a futuro). |
| S9 | Los datos académicos y financieros oficiales se leen del ERP; Vista 360° no los recalcula. |
