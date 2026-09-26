# GastroReserva — matriz de actores, objetivos y responsabilidades

## Actores

| Actor | Objetivo | Responsabilidades | Operaciones principales |
|---|---|---|---|
| Administrador | Configurar y supervisar el sistema completo. | Gestionar usuarios, catálogos, zonas, mesas, turnos y parámetros; revisar operación, trazabilidad e indicadores. | Administración de usuarios; escritura de zonas, mesas y turnos; consulta y transición de reservas; futura gestión de carta y reportes. |
| Host/recepción | Coordinar reservas y llegada de clientes. | Mantener clientes, consultar agenda, registrar reservas telefónicas, confirmar/cancelar, realizar check-in y asignar o reasignar mesas. | Consulta de zonas, mesas y turnos; gestión de clientes; alta operativa, transición, check-in y reasignación trazable de reservas. |
| Mesero | Gestionar la atención de las mesas asignadas. | Consultar contexto de mesa y reserva, registrar pedidos, actualizar estados y finalizar la atención permitida. | Consulta operativa y transición de reservas; futuras operaciones de pedido y estado desde móvil. |
| Cliente | Autogestionar su relación con el restaurante. | Registrarse, mantener su perfil, consultar disponibilidad, crear/cancelar reservas, consultar sus reservas y registrar feedback posterior. | Registro/login; perfil propio; disponibilidad; creación, consulta y cancelación de reservas propias; futuro feedback. |

## Matriz de responsabilidad por módulo

Leyenda: **R** ejecuta, **A** responde por la configuración o supervisión, **C** consulta/participa y **—** sin responsabilidad directa.

| Módulo o proceso | Administrador | Host | Mesero | Cliente |
|---|:---:|:---:|:---:|:---:|
| Usuarios y roles | A/R | — | — | C, sólo registro propio |
| Clientes | A | R | C | R, sólo perfil propio |
| Zonas y mesas | A/R | C | C | — |
| Turnos y capacidad | A/R | C | C | C mediante disponibilidad |
| Disponibilidad | C | C | C | R |
| Reservas | A | R | C | R, sólo propias |
| Check-in y asignación | A | R | C | C |
| Reasignación de mesa | A | R | C | C |
| Carta | A/R | C | C | C |
| Pedidos y atención | A | C | R | C |
| Finalización de visita | A | C | R | C |
| Feedback | C | — | — | R |
| Ocupación y no-show | A/R | C | — | — |
| Resumen de IA | A/R | — | — | — |

## Permisos implementados actualmente en el backend

| Operación | Administrador | Host | Mesero | Cliente | Anónimo |
|---|:---:|:---:|:---:|:---:|:---:|
| Salud, login y registro de cliente | Sí | Sí | Sí | Sí | Sí |
| Administrar usuarios | Sí | No | No | No | No |
| Consultar zonas y mesas | Sí | Sí | Sí | No | No |
| Crear/actualizar zonas y mesas | Sí | No | No | No | No |
| Gestionar clientes | Sí | Sí | No | Sólo perfil propio | No |
| Consultar turnos | Sí | Sí | Sí | Mediante disponibilidad | No |
| Crear/actualizar turnos | Sí | No | No | No | No |
| Consultar disponibilidad | Sí | Sí | Sí | Sí | No |
| Consultar agenda completa | Sí | Sí | Sí | Sólo reservas propias | No |
| Crear reserva | Para un cliente | Para un cliente | No | Para sí mismo | No |
| Cambiar estado de reserva | Sí | Sí | Sí | Sólo cancelar la propia | No |
| Check-in y reasignar mesa | Sí | Sí | No | No | No |
| Consultar historial | Sí | Sí | Sí | No | No |

## Separación de identidades

`Usuario` representa credenciales, rol y estado de acceso. `Cliente` representa información de negocio. Una cuenta con rol `CLIENTE` se vincula uno a uno con un perfil de cliente, pero recepción también puede registrar clientes presenciales sin crearles credenciales. Esta separación evita almacenar contraseñas o permisos dentro de la entidad de negocio.

## Responsabilidades aún pendientes de implementación

- El mesero todavía no dispone de carta, pedido y estados de atención.
- El cliente todavía no puede registrar feedback.
- El administrador todavía no dispone de reportes ni resumen de comentarios con IA.
