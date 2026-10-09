# GastroReserva — modelo conceptual, DER lógico y diccionario de datos

## Decisiones de modelado

1. `Usuario` contiene credenciales y rol; `Cliente` contiene datos del dominio. La relación uno a uno es opcional para admitir clientes registrados por recepción sin cuenta.
2. `Reserva` conserva `inicio` y `fin` además de la fecha/turno. Esto vuelve explícito el intervalo validado y permite manejar turnos nocturnos.
3. Los estados de reserva se almacenan como valores controlados y `HistorialReserva` conserva cada transición con actor y fecha.
4. Los catálogos usan estado activo en lugar de eliminación física para no romper referencias históricas.
5. `ItemPedido` copia el precio vigente del producto para cumplir RN-05.
6. `Feedback` se vincula a una reserva finalizada para verificar propiedad y RN-07.

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
    CLIENTE o|--o{ HISTORIAL_RESERVA : "cliente anterior"
    CLIENTE o|--o{ HISTORIAL_RESERVA : "cliente nuevo"
    RESERVA ||--o| PEDIDO : "habilita"
    MESA ||--o{ MESA_ABIERTA : "atiende sin reserva"
    MESA_ABIERTA ||--o| PEDIDO : "habilita alternativamente"
    USUARIO ||--o{ PEDIDO : "responsable"
    PEDIDO ||--o{ ITEM_PEDIDO : "contiene"
    PEDIDO ||--o{ HISTORIAL_PEDIDO : "audita"
    USUARIO ||--o{ HISTORIAL_PEDIDO : "realiza"
    USUARIO ||--o{ FEEDBACK : "escribe"
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
        bigint cliente_anterior_id FK
        bigint cliente_nuevo_id FK
        string observaciones_anteriores
        string observaciones_nuevas
        bigint cambiado_por_usuario_id FK
        datetime creado_en
    }
    PRODUCTO_MENU {
        bigint id PK
        string nombre
        string nombre_normalizado UK
        decimal precio
        boolean disponible
    }
    PEDIDO {
        bigint id PK
        bigint reserva_id FK_UK
        bigint mesa_abierta_id FK_UK
        bigint responsable_id FK
        string estado
    }
    ITEM_PEDIDO {
        bigint id PK
        bigint pedido_id FK
        bigint producto_menu_id FK
        integer cantidad
        decimal precio_unitario_historico
    }
    MESA_ABIERTA {
        bigint id PK
        bigint mesa_id FK
        bigint mesa_activa_id FK_UK
        integer cantidad_personas
        datetime inicio
        datetime fin_previsto
    }
    HISTORIAL_PEDIDO {
        bigint id PK
        bigint pedido_id FK
        bigint cambiado_por_id FK
        string estado_anterior
        string estado_nuevo
        string detalle
        datetime creado_en
    }
    FEEDBACK {
        bigint id PK
        bigint reserva_id FK_UK
        bigint autor_id FK
        integer puntuacion
        string comentario
    }
```

`ProductoMenu` se migró en V6; apertura, pedido, ítem e historial de pedido en V7; feedback en V8. `EstadoPedido` es un enum Java almacenado como VARCHAR con CHECK, no una tabla. El pedido tiene exactamente un origen (reserva o apertura); su mesa se deriva de ese origen y su total se calcula desde los ítems, sin duplicar esos datos. Puede estar inicialmente vacío. No incluye facturación.

## DER lógico implementado — Flyway V1 a V9

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
    CLIENTES o|--o{ HISTORIAL_RESERVAS : "cliente_anterior_id"
    CLIENTES o|--o{ HISTORIAL_RESERVAS : "cliente_nuevo_id"
    MESAS ||--o{ MESAS_ABIERTAS : "mesa_id"
    USUARIOS ||--o{ MESAS_ABIERTAS : "creada_por_id"
    USUARIOS o|--o{ MESAS_ABIERTAS : "cerrada_por_id"
    RESERVAS o|--o| PEDIDOS : "reserva_id"
    MESAS_ABIERTAS o|--o| PEDIDOS : "mesa_abierta_id"
    USUARIOS ||--o{ PEDIDOS : "responsable_id"
    PEDIDOS ||--o{ ITEMS_PEDIDO : "pedido_id"
    PRODUCTOS_MENU ||--o{ ITEMS_PEDIDO : "producto_menu_id"
    PEDIDOS ||--o{ HISTORIAL_PEDIDOS : "pedido_id"
    USUARIOS ||--o{ HISTORIAL_PEDIDOS : "cambiado_por_id"
    RESERVAS ||--o| FEEDBACK : "reserva_id"
    USUARIOS ||--o{ FEEDBACK : "autor_id"

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
        bigint cliente_anterior_id FK
        bigint cliente_nuevo_id FK
        varchar observaciones_anteriores
        varchar observaciones_nuevas
        bigint cambiado_por_usuario_id FK
        timestamptz creado_en
    }
    PRODUCTOS_MENU {
        bigint id PK
        varchar nombre
        varchar nombre_normalizado UK
        varchar descripcion
        numeric precio
        boolean disponible
        timestamptz creado_en
        timestamptz actualizado_en
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
| tipo_evento | VARCHAR(30) | No | `CREACION`, `CAMBIO_ESTADO`, `CHECK_IN`, `REASIGNACION` o `CORRECCION_CLIENTE` (V9). |
| estado_anterior | VARCHAR(20) | Sí | Nulo únicamente en el evento de creación. |
| estado_nuevo | VARCHAR(20) | No | Estado resultante. |
| motivo | VARCHAR(500) | Sí | Explicación operativa. |
| mesa_anterior_id | BIGINT | Sí | FK a `mesas`; obligatoria para check-in/reasignación. |
| mesa_nueva_id | BIGINT | Sí | FK a `mesas`; registra asignación inicial, check-in o destino de reasignación. |
| cliente_anterior_id / cliente_nuevo_id | BIGINT | Sí | FKs a `clientes`; obligatorias sólo en CORRECCION_CLIENTE. Pueden coincidir si sólo cambian observaciones. |
| observaciones_anteriores / observaciones_nuevas | VARCHAR(500) | Sí | Instantánea de la nota antes/después de la corrección; NULL representa nota vacía. |
| cambiado_por_usuario_id | BIGINT | No | FK al actor responsable. |
| creado_en | TIMESTAMPTZ | No | Fecha/hora inmutable del evento. |

Índices: `(reserva_id, creado_en)`, `mesa_anterior_id`, `mesa_nueva_id`, `cliente_anterior_id` y `cliente_nuevo_id`. Los eventos se agregan cronológicamente; no existe contrato de actualización o eliminación del historial.

V9 agrega las cuatro columnas sin reescribir V1–V8. CHECK exige para CORRECCION_CLIENTE ambos clientes, motivo no vacío, estado anterior/nuevo SOLICITADA y mesas nulas; para otros eventos las cuatro columnas nuevas son nulas. El servicio sólo permite la corrección a administrador/host, exige cliente activo y versión vigente, y mantiene los campos de planificación. La FK principal sigue siendo `reservas.cliente_id`: es la relación actual; las FKs del historial son evidencia del antes/después, no propietarios simultáneos.

### `productos_menu`

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK, identidad estable del producto. |
| nombre | VARCHAR(120) | No | Nombre visible, sin espacios exteriores y no vacío. |
| nombre_normalizado | VARCHAR(120) | No | UK, nombre en minúsculas; evita duplicados aun ante altas concurrentes. No se expone en la API. |
| descripcion | VARCHAR(500) | Sí | Descripción opcional. |
| precio | NUMERIC(12,2) | No | Positivo; máximo 10 enteros y 2 decimales. Java usa BigDecimal. |
| disponible | BOOLEAN | No | Sólo los disponibles aparecen en `/api/carta`; permite retirar/restaurar sin borrar. |
| creado_en | TIMESTAMPTZ | No | Fecha de creación. |
| actualizado_en | TIMESTAMPTZ | No | Fecha de última modificación. |

V6 crea esta tabla y su índice `(disponible, nombre)`. V7 la referencia desde `items_pedido`, que copia nombre y precio al agregar el producto (RN-05). Cambiar el catálogo no modifica registros históricos.

### `mesas_abiertas` — V7

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK identidad de la atención. |
| mesa_id | BIGINT | No | FK mesas; permanece al cerrar para conservar historial. |
| mesa_activa_id | BIGINT | Sí | FK única mesas; mientras está abierta coincide con mesa_id; al cerrar pasa a NULL. |
| cantidad_personas | INTEGER | No | Positiva; servicio valida capacidad. |
| inicio / fin_previsto | TIMESTAMP | No | Horario local, fin mayor que inicio. No cierra automáticamente la mesa. |
| creada_por_id | BIGINT | No | FK usuarios, actor de apertura. |
| cerrada_por_id | BIGINT | Sí | FK usuarios, actor de cierre. |
| creada_en / cerrada_en | TIMESTAMPTZ | Sólo cierre | Marcas de auditoría. |

CHECK vincula estado activo con campos de cierre; sólo una atención abierta por mesa. Índice (mesa_id, inicio, fin_previsto). El servicio impide abrir mesa ocupada y conflictos con reservas.

### `pedidos` — V7

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| reserva_id | BIGINT | Sí | FK única reservas. |
| mesa_abierta_id | BIGINT | Sí | FK única mesas_abiertas. |
| responsable_id | BIGINT | No | FK usuarios, responsable operativo. |
| estado | VARCHAR(20) | No | CHECK: ABIERTO, EN_PREPARACION, SERVIDO, CERRADO, CANCELADO. |
| creado_en / actualizado_en | TIMESTAMPTZ | No | Auditoría. |

CHECK de origen exclusivo: exactamente reserva_id o mesa_abierta_id debe estar presente. La reserva debe estar SENTADA o la atención abierta; esta comprobación transaccional es del servicio. Índice (responsable_id, estado). Una sola orden por origen, incluso si quedó cancelada. Mesa y total son **derivados**, no columnas redundantes.

### `items_pedido` — V7

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| pedido_id | BIGINT | No | FK pedidos, índice. |
| producto_menu_id | BIGINT | No | FK productos_menu, producto original. |
| nombre_producto | VARCHAR(120) | No | Copia del nombre al agregar. |
| cantidad | INTEGER | No | CHECK 1–999. |
| precio_unitario_historico | NUMERIC(12,2) | No | Positivo, copia del precio al agregar. |

El mismo producto puede tener varias líneas con distintos precios históricos. El total operativo es suma de cantidad × precio de cada línea (BigDecimal). Retirar una línea sólo se permite con pedido ABIERTO y registra sus datos en historial; no borra la identidad del producto.

### `historial_pedidos` — V7

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| pedido_id | BIGINT | No | FK pedidos; índice (pedido_id, id). |
| estado_anterior | VARCHAR(20) | Sí | NULL al crear; CHECK de estados. |
| estado_nuevo | VARCHAR(20) | No | CHECK de estados. |
| detalle | VARCHAR(500) | No | Motivo o descripción del cambio de ítem/responsable. |
| cambiado_por_id | BIGINT | No | FK usuarios. |
| creado_en | TIMESTAMPTZ | No | Fecha inmutable. |

Es append-only por API: no hay endpoint para editar/borrar eventos. Los cambios de ítems o responsable pueden mantener el mismo estado anterior/nuevo.

### `feedback` — V8

| Columna | Tipo | Nulo | Restricción / significado |
|---|---|:---:|---|
| id | BIGINT | No | PK. |
| reserva_id | BIGINT | No | FK única reservas: una opinión por visita. |
| autor_id | BIGINT | No | FK usuarios, propietario de la reserva validado en servicio. |
| puntuacion | INTEGER | No | CHECK 1–5. |
| comentario | VARCHAR(1000) | No | No vacío tras trim. |
| creado_en | TIMESTAMPTZ | No | Fecha de opinión. |

Índices (autor_id, creado_en) y (creado_en, puntuacion). Estado FINALIZADA y propiedad se comprueban con la reserva bloqueada. No hay columnas de IA: la clasificación futura está aplazada.

## Notas de integridad

- Hay 13 tablas de dominio más `flyway_schema_history`, que es infraestructura, no una entidad del restaurante.
- No existen tablas de facturas, pagos ni contabilidad.
- Reportes son agregados; no tienen tabla propia. La consulta de notificaciones reutiliza el historial de reservas y sólo devuelve eventos del cliente.
- Las restricciones únicas protegen identidades, origen de pedido y feedback; permisos y transiciones corresponden al backend.
