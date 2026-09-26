# GastroReserva — modelo conceptual, DER lógico y diccionario de datos

## Decisiones de modelado

1. `Usuario` contiene credenciales y rol; `Cliente` contiene datos del dominio. La relación uno a uno es opcional para admitir clientes registrados por recepción sin cuenta.
2. `Reserva` conserva `inicio` y `fin` además de la fecha/turno. Esto vuelve explícito el intervalo validado y permite manejar turnos nocturnos.
3. Los estados de reserva se almacenan como valores controlados y `HistorialReserva` conserva cada transición con actor y fecha.
4. Los catálogos usan estado activo en lugar de eliminación física para no romper referencias históricas.
5. El futuro `ItemPedido` copiará el precio vigente del producto para cumplir RN-05.
6. El futuro `Feedback` se vinculará a una reserva finalizada para verificar propiedad y RN-07.

## Modelo conceptual del alcance completo

```mermaid
erDiagram
    USUARIO o|--o| CLIENTE : "puede habilitar"
    ZONA ||--o{ MESA : "agrupa"
    CLIENTE ||--o{ RESERVA : "realiza"
    TURNO ||--o{ RESERVA : "programa"
    MESA ||--o{ RESERVA : "reserva"
    USUARIO ||--o{ RESERVA : "crea"
    RESERVA ||--o{ HISTORIAL_RESERVA : "registra"
    USUARIO ||--o{ HISTORIAL_RESERVA : "ejecuta cambio"
    MESA o|--o{ HISTORIAL_RESERVA : "mesa anterior"
    MESA o|--o{ HISTORIAL_RESERVA : "mesa nueva"
    RESERVA ||--o| PEDIDO : "habilita"
    MESA ||--o{ PEDIDO : "recibe"
    ESTADO_PEDIDO ||--o{ PEDIDO : "clasifica"
    PEDIDO ||--|{ ITEM_PEDIDO : "contiene"
    PRODUCTO_MENU ||--o{ ITEM_PEDIDO : "referencia"
    RESERVA ||--o| FEEDBACK : "recibe al finalizar"

    USUARIO {
        bigint id PK
        string email UK
        string password_hash
        string rol
        boolean activo
    }
    CLIENTE {
        bigint id PK
        bigint usuario_id FK_UK
        string nombre
        string email UK
        string telefono
        boolean activo
    }
    ZONA {
        bigint id PK
        string nombre UK
        boolean activa
    }
    MESA {
        bigint id PK
        bigint zona_id FK
        integer numero UK
        integer capacidad
        string estado
        boolean activa
    }
    TURNO {
        bigint id PK
        string nombre UK
        time hora_inicio
        time hora_fin
        integer capacidad_maxima
        boolean activo
    }
    RESERVA {
        bigint id PK
        bigint cliente_id FK
        bigint turno_id FK
        bigint mesa_id FK
        date fecha
        datetime inicio
        datetime fin
        integer cantidad_personas
        string estado
    }
    HISTORIAL_RESERVA {
        bigint id PK
        bigint reserva_id FK
        string tipo_evento
        string estado_anterior
        string estado_nuevo
        bigint mesa_anterior_id FK
        bigint mesa_nueva_id FK
        bigint cambiado_por_usuario_id FK
        datetime creado_en
    }
    PRODUCTO_MENU {
        bigint id PK
        string nombre
        decimal precio
        boolean disponible
    }
    PEDIDO {
        bigint id PK
        bigint reserva_id FK_UK
        bigint mesa_id FK
        bigint estado_pedido_id FK
        decimal total_operativo
    }
    ITEM_PEDIDO {
        bigint id PK
        bigint pedido_id FK
        bigint producto_menu_id FK
        integer cantidad
        decimal precio_unitario_historico
    }
    ESTADO_PEDIDO {
        bigint id PK
        string codigo UK
        string nombre
    }
    FEEDBACK {
        bigint id PK
        bigint reserva_id FK_UK
        integer puntuacion
        string comentario
    }
```

`ProductoMenu`, `Pedido`, `ItemPedido`, `EstadoPedido` y `Feedback` son conceptos contractuales diseñados pero aún no migrados. Sus atributos definitivos se cerrarán al implementar RN-04 a RN-07, sin ampliar el alcance a facturación.

## DER lógico implementado — Flyway V1 a V5

```mermaid
erDiagram
    USUARIOS o|--o| CLIENTES : "usuario_id"
    ZONAS ||--o{ MESAS : "zona_id"
    CLIENTES ||--o{ RESERVAS : "cliente_id"
    TURNOS ||--o{ RESERVAS : "turno_id"
    MESAS ||--o{ RESERVAS : "mesa_id"
    USUARIOS ||--o{ RESERVAS : "creado_por_usuario_id"
    RESERVAS ||--o{ HISTORIAL_RESERVAS : "reserva_id"
    USUARIOS ||--o{ HISTORIAL_RESERVAS : "cambiado_por_usuario_id"
    MESAS o|--o{ HISTORIAL_RESERVAS : "mesa_anterior_id"
    MESAS o|--o{ HISTORIAL_RESERVAS : "mesa_nueva_id"

    USUARIOS {
        bigint id PK
        varchar nombre
        varchar email UK
        varchar password_hash
        varchar rol
        boolean activo
        timestamptz creado_en
        timestamptz actualizado_en
    }
    CLIENTES {
        bigint id PK
        varchar nombre
        varchar email UK
        varchar telefono
        boolean activo
        bigint usuario_id FK_UK
        timestamptz creado_en
        timestamptz actualizado_en
    }
    ZONAS {
        bigint id PK
        varchar nombre UK
        varchar descripcion
        boolean activa
    }
    MESAS {
        bigint id PK
        integer numero UK
        integer capacidad
        varchar estado
        boolean activa
        bigint zona_id FK
    }
    TURNOS {
        bigint id PK
        varchar nombre UK
        time hora_inicio
        time hora_fin
        integer capacidad_maxima
        boolean activo
        timestamptz creado_en
        timestamptz actualizado_en
    }
    RESERVAS {
        bigint id PK
        bigint cliente_id FK
        bigint turno_id FK
        bigint mesa_id FK
        date fecha
        timestamp inicio
        timestamp fin
        integer cantidad_personas
        varchar estado
        varchar observaciones
        bigint creado_por_usuario_id FK
        bigint version
        timestamptz creado_en
        timestamptz actualizado_en
    }
    HISTORIAL_RESERVAS {
        bigint id PK
        bigint reserva_id FK
        varchar tipo_evento
        varchar estado_anterior
        varchar estado_nuevo
        varchar motivo
        bigint mesa_anterior_id FK
        bigint mesa_nueva_id FK
        bigint cambiado_por_usuario_id FK
        timestamptz creado_en
    }
```

## Diccionario de datos inicial

### `usuarios`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK, identidad. |
| nombre | VARCHAR(120) | No | Nombre visible del usuario. |
| email | VARCHAR(255) | No | Único; identificador de login normalizado. |
| password_hash | VARCHAR(100) | No | Hash BCrypt; nunca forma parte de un DTO de salida. |
| rol | VARCHAR(30) | No | `ADMINISTRADOR`, `HOST`, `MESERO` o `CLIENTE`. |
| activo | BOOLEAN | No | Habilita o bloquea autenticación/autorización. |
| creado_en | TIMESTAMPTZ | No | Auditoría de creación. |
| actualizado_en | TIMESTAMPTZ | No | Auditoría de última actualización. |

Índice: `(rol, activo)` para administración y resolución operativa.

### `clientes`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK, identidad del cliente. |
| nombre | VARCHAR(120) | No | Nombre de negocio. |
| email | VARCHAR(255) | No | Único; permite vincular un registro presencial con una cuenta posterior. |
| telefono | VARCHAR(30) | Sí | Contacto opcional. |
| activo | BOOLEAN | No | Habilita nuevas operaciones del cliente. |
| usuario_id | BIGINT | Sí | FK única a `usuarios`; sólo existe si tiene cuenta vinculada. |
| creado_en | TIMESTAMPTZ | No | Auditoría de creación. |
| actualizado_en | TIMESTAMPTZ | No | Auditoría de actualización. |

Índice: `(activo, nombre)` para búsquedas de recepción.

### `zonas`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| nombre | VARCHAR(255) | No | Único; identifica el sector. |
| descripcion | VARCHAR(255) | Sí | Descripción operativa. |
| activa | BOOLEAN | No | Una zona inactiva no ofrece mesas para nuevas reservas. |

### `mesas`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| numero | INTEGER | No | Único y positivo. |
| capacidad | INTEGER | No | Positiva; protege RN-01. |
| estado | VARCHAR(255) | No | `DISPONIBLE` u `OCUPADA`; check-in ocupa y finalización libera. |
| activa | BOOLEAN | No | Control administrativo. |
| zona_id | BIGINT | No | FK a `zonas`. |

Índice: `zona_id`. No se permite reducir capacidad por debajo de una reserva activa.

### `turnos`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| nombre | VARCHAR(100) | No | Único. |
| hora_inicio | TIME | No | Inicio local del intervalo. |
| hora_fin | TIME | No | Fin local; puede corresponder al día siguiente. |
| capacidad_maxima | INTEGER | No | Positiva; límite agregado por fecha. |
| activo | BOOLEAN | No | Disponibilidad administrativa. |
| creado_en | TIMESTAMPTZ | No | Auditoría de creación. |
| actualizado_en | TIMESTAMPTZ | No | Auditoría de actualización. |

Índice: `(activo, hora_inicio, hora_fin)`. Inicio y fin no pueden ser iguales.

### `reservas`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| cliente_id | BIGINT | No | FK a `clientes`. |
| turno_id | BIGINT | No | FK a `turnos`. |
| mesa_id | BIGINT | No | FK a `mesas`; recurso protegido frente a solapamientos. |
| fecha | DATE | No | Fecha operativa del turno. |
| inicio | TIMESTAMP | No | Inicio local materializado. |
| fin | TIMESTAMP | No | Fin local materializado; debe ser posterior a inicio. |
| cantidad_personas | INTEGER | No | Positiva; validada contra mesa y turno. |
| estado | VARCHAR(20) | No | Estado contractual de RN-03. |
| observaciones | VARCHAR(500) | Sí | Nota operativa sin información de pago/fiscal. |
| creado_por_usuario_id | BIGINT | No | FK al actor que registró la reserva. |
| version | BIGINT | No | Control optimista de cambios concurrentes. |
| creado_en | TIMESTAMPTZ | No | Auditoría de creación. |
| actualizado_en | TIMESTAMPTZ | No | Auditoría de actualización. |

Índices: `(mesa_id, inicio, fin, estado)`, `(turno_id, fecha, estado)` y `(cliente_id, fecha, estado)`.

### `historial_reservas`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| reserva_id | BIGINT | No | FK a `reservas`. |
| tipo_evento | VARCHAR(30) | No | `CREACION`, `CAMBIO_ESTADO`, `CHECK_IN` o `REASIGNACION`. |
| estado_anterior | VARCHAR(20) | Sí | Nulo únicamente en el evento de creación. |
| estado_nuevo | VARCHAR(20) | No | Estado resultante. |
| motivo | VARCHAR(500) | Sí | Explicación operativa. |
| mesa_anterior_id | BIGINT | Sí | FK a `mesas`; obligatoria para check-in/reasignación. |
| mesa_nueva_id | BIGINT | Sí | FK a `mesas`; registra asignación inicial, check-in o destino de reasignación. |
| cambiado_por_usuario_id | BIGINT | No | FK al actor responsable. |
| creado_en | TIMESTAMPTZ | No | Fecha/hora inmutable del evento. |

Índices: `(reserva_id, creado_en)`, `mesa_anterior_id` y `mesa_nueva_id`. Los eventos se agregan cronológicamente; no existe contrato de actualización o eliminación del historial.

## Conceptos pendientes y persistencia prevista

| Concepto | Persistencia prevista | Justificación |
|---|---|---|
| ProductoMenu | Tabla de catálogo con precio decimal y disponibilidad. | Carta básica y referencia estable desde ítems. |
| EstadoPedido | Catálogo o valor controlado definido al cerrar la máquina de estados. | Evitar estados libres y proteger transiciones. |
| Pedido | Tabla vinculada a reserva sentada o mesa operativamente abierta. | Encabeza consumo y total operativo. |
| ItemPedido | Tabla detalle con producto, cantidad y precio histórico. | Normalización uno-a-muchos y RN-05. |
| Feedback | Tabla con FK única a reserva y datos de valoración/comentario. | Verificar propiedad, visita finalizada y evitar duplicados según contrato final. |

No se crearán tablas de facturas, pagos, cuentas contables ni documentos fiscales porque están fuera del alcance cerrado.
