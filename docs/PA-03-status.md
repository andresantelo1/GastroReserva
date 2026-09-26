# PA-03 GastroReserva — estado de cobertura

Diagnóstico inicial: **2026-08-15**. Esta matriz compara el repositorio actual con el enunciado oficial PA-03. Los estados significan: **completo** (implementado y verificado en el alcance actual), **parcial** (existe una base insuficiente), **ausente** (no existe implementación) y **bloqueado** (no puede verificarse localmente por una dependencia externa).

## Línea base e infraestructura

| Área | Estado | Evidencia / brecha |
|---|---|---|
| Java 21 | Parcial | `pom.xml` e IntelliJ usan Java 21 y Temurin 21 está instalado; el `PATH` global todavía selecciona Java 8. |
| Spring Boot | Parcial | Backend Spring Boot 4.1.0 con salud, usuarios/auth, clientes, zonas, mesas, turnos, disponibilidad, reservas, check-in y reasignación; faltan los demás módulos. |
| PostgreSQL | Completo | Conexión real verificada manualmente con zonas y mesas; configuración externa sin credenciales versionadas. |
| Migraciones | Parcial | Flyway V1 a V5 cubre usuarios, clientes, zonas, mesas, turnos, reservas e historial ampliado para check-in/reasignación; faltan las tablas del resto del dominio. |
| Autenticación y roles | Parcial | Registro con perfil `Cliente`, BCrypt, JWT y roles vigentes aplicados al backend; faltan las experiencias web/móvil. |
| Pruebas | Parcial | 39 pruebas cubren contexto, migraciones, controladores, servicios, RN-01 a RN-03, RN-08, JWT y permisos; faltan pedidos y clientes finales. |
| React + TypeScript web | Ausente | No existe aplicación web. |
| React Native + TypeScript | Ausente | No existe aplicación móvil. |
| Docker / Compose | Bloqueado | No hay archivos Docker y el comando `docker` no está instalado en este equipo. |
| GitHub Actions | Ausente | No existe `.github/workflows`. |
| Git | Bloqueado | La carpeta abierta no contiene `.git`; no hay rama, estado ni historial que inspeccionar. |
| Node / npm | Bloqueado | No están instalados o disponibles en `PATH`; será necesario para web y móvil. |
| README / variables | Completo | `README.md` y `.env.example` documentan el backend actual sin almacenar secretos. |
| Documentación Parcial 1 | Completo | Visión, actores, RF/RNF, RN, casos/mapa, DER/diccionario, arquitectura y backlog están indexados en `docs/README.md`. |

## Alcance funcional y modelo mínimo

| Módulo / concepto | Estado | Evidencia / brecha |
|---|---|---|
| Clientes / `Cliente` | Parcial | Modelo separado de seguridad, persistencia, perfil propio, filtros, creación/actualización, vinculación por correo e integración con reservas; falta consumo web/móvil. |
| Usuarios y roles | Parcial | Usuario, cuatro roles, registro, login, administración y permisos backend; las cuentas cliente quedan vinculadas con `Cliente`, pero faltan flujos web/móvil. |
| Zonas / `Zona` | Parcial | DTO, servicio, validación, filtros, GET/POST/PUT, errores y permisos cubiertos; falta consumo web/móvil. |
| Mesas / `Mesa` | Parcial | DTO, servicio, relación con zona, filtros, GET/POST/PUT, permisos y estados operativos `DISPONIBLE/OCUPADA` integrados con check-in; faltan clientes web/móvil. |
| Turnos / `Turno` | Parcial | Modelo, horarios incluso nocturnos, capacidad, estado, filtros, GET/POST/PUT, permisos e integración con disponibilidad/reservas; falta consumo web/móvil. |
| Reservas / `Reserva` | Parcial | Disponibilidad, alta propia/operativa, agenda, capacidad, intervalos, estados, cancelación, check-in, reasignación y prevención de solapamientos implementados; faltan clientes web/móvil. |
| `HistorialReserva` | Parcial | Creación, transición, check-in y reasignación registran tipo, estados, mesa anterior/nueva cuando corresponde, motivo, fecha y usuario; falta consumo web/móvil. |
| Carta / `ProductoMenu` | Ausente | Sin modelo, disponibilidad ni API. |
| Pedidos / `Pedido` | Ausente | Sin modelo, total operativo ni API. |
| `ItemPedido` | Ausente | Sin precio histórico. |
| `EstadoPedido` / atención | Ausente | Sin catálogo/transiciones. |
| `Feedback` | Ausente | Sin restricción posterior a visita. |
| Panel y reportes | Ausente | Sin indicadores de ocupación/no-show. |

## Reglas de negocio

| Regla | Estado | Brecha |
|---|---|---|
| RN-01 capacidad de mesa y turno | Completo | Creación y disponibilidad validan capacidad individual y suma del turno, con bloqueo pesimista y pruebas de error/éxito. |
| RN-02 evitar solapamientos | Completo | Los intervalos bloqueantes de una misma mesa no pueden cruzarse; turnos nocturnos y liberación por cancelación están probados. |
| RN-03 estados de reserva | Completo | Enum contractual y máquina de estados con transiciones válidas, finales inmutables, auditoría y pruebas. |
| RN-04 pedido sólo para reserva sentada o mesa abierta | Ausente | No existe pedido ni apertura de mesa. |
| RN-05 precio histórico de ítems | Ausente | No existe `ItemPedido`. |
| RN-06 sólo total operativo | Ausente | No existe pedido; se mantiene explícitamente fuera del alcance fiscal. |
| RN-07 feedback tras finalizar | Ausente | No existen visita finalizada ni feedback. |
| RN-08 trazabilidad de reasignación | Completo | Check-in y reasignación registran reserva, mesa anterior/nueva, motivo, actor y fecha; validan actividad, estado operativo, capacidad y solapamiento con bloqueo transaccional. |

## Requisitos funcionales RF-01 a RF-20

| ID | Estado | Evidencia / brecha |
|---|---|---|
| RF-01 | Parcial | Registro, login JWT, BCrypt y permisos por rol vigente verificados en backend; faltan login/sesión web y móvil. |
| RF-02 | Parcial | Usuarios, clientes, zonas, mesas, turnos y reservas admiten filtros relevantes y listas vacías; faltan módulos restantes y estados vacíos web/móvil. |
| RF-03 | Parcial | Bean Validation y errores REST uniformes cubren los módulos actuales; faltan módulos restantes y validación en clientes web/móvil. |
| RF-04 | Parcial | Transiciones, check-in y reasignaciones registran fecha, usuario, tipo, estados, mesas y motivo; faltan pedidos/atención. |
| RF-05 | Parcial | Backend de clientes con alta, consulta, actualización, filtros, perfil propio, validación, permisos e integración con reservas; faltan experiencias web/móvil. |
| RF-06 | Parcial | Zonas y mesas tienen contratos DTO, filtros, consulta, creación, actualización, estados operativos `DISPONIBLE/OCUPADA` y permisos verificados; falta experiencia web/móvil. |
| RF-07 | Parcial | Backend de turnos con consulta, filtros, creación, actualización, capacidad, estado, validación, permisos e integración con reservas; falta consumo web/móvil. |
| RF-08 | Parcial | Backend de disponibilidad, alta, consulta, filtros, cancelación, estados, check-in, reasignación e historial implementado; faltan experiencias web/móvil. |
| RF-09 | Parcial | Check-in backend protegido pasa sólo `CONFIRMADA` a `SENTADA`, valida/asigna mesa, ocupa el recurso y audita; falta UI. |
| RF-10 | Ausente | Carta básica inexistente. |
| RF-11 | Ausente | Pedidos inexistentes. |
| RF-12 | Ausente | Estados de atención inexistentes. |
| RF-13 | Ausente | Feedback inexistente. |
| RF-14 | Ausente | Panel y reportes inexistentes. |
| RF-15 | Completo | El backend rechaza reservas sin capacidad de mesa o turno y lo demuestra con pruebas. |
| RF-16 | Completo | El backend rechaza reservas solapadas y libera el intervalo al cancelar. |
| RF-17 | Parcial | Asignación/reasignación backend valida mesa y registra trazabilidad completa; falta experiencia web de host. |
| RF-18 | Ausente | Sin pedidos o cálculo de totales. |
| RF-19 | Ausente | Sin móvil ni transición de mesero. |
| RF-20 | Ausente | Sin indicadores de ocupación/no-show. |

## Experiencias, API y flujo crítico

| Área | Estado | Brecha |
|---|---|---|
| Experiencia web (H) | Ausente | No hay mapa de mesas, agenda, turnos, check-in, carta, pedidos ni panel. |
| Experiencia móvil (I) | Ausente | No hay búsqueda, reservas, notificaciones, feedback ni vista de mesero. |
| Flujo crítico (J) | Parcial | Ya funciona `cliente consulta disponibilidad → reserva → recepción confirma y realiza check-in/asignación`; faltan pedido, finalización completa y feedback en las experiencias finales. |
| API REST (K) | Parcial | Auth/login, usuarios, clientes, zonas, mesas, turnos, reservas, check-in y reasignación con DTO, búsquedas, transiciones, permisos y errores; faltan recursos restantes. |
| Criterios globales (L) | Parcial | Capacidad y solapamientos tienen evidencia automatizada de éxito/error; faltan los demás criterios globales. |
| Spring AI acotado (M) | Ausente | Sin puerto, proveedor, timeout ni fallback. |

## Diferencias relevantes encontradas

1. La implementación heredada exponía entidades JPA; esto ya fue corregido para clientes, zonas, mesas y turnos mediante DTO y servicios.
2. Hibernate usaba `ddl-auto: update` sin migraciones; el incremento de base lo reemplazó por Flyway + validación.
3. No existe repositorio Git en la carpeta, por lo que todavía no hay trazabilidad por commits o ramas.
4. El entorno global usa Java 8 y no dispone de Node/npm ni Docker, aunque JDK 21 sí está instalado para IntelliJ.

## Incrementos recomendados

1. Implementar carta básica y continuar con mesa abierta, pedidos y precios históricos para RN-04 a RN-06.
2. Completar atención/finalización, feedback y reportes antes de cerrar los flujos finales.
3. Construir las experiencias web/móvil sobre contratos protegidos y estables.

La matriz debe actualizarse después de cada incremento y nunca se debe marcar un requisito como completo sin pruebas o evidencia equivalente.

## Registro de verificación

### 2026-08-15 — incremento de base técnica

- Línea base previa: `mvn test` compiló con Java 21, pero falló la única prueba porque dependía de PostgreSQL y no había credencial disponible.
- Corrección de reproducibilidad: el wrapper de Windows quedó compatible con una carpeta `.m2` normal y se documentó cómo activar el JDK 21 ya instalado.
- Migración: Flyway ejecuta `V1__create_zonas_and_mesas.sql` sobre una base efímera y Hibernate valida el esquema resultante.
- Comando de verificación: `.\mvnw.cmd test`.
- Resultado: compilación exitosa y pruebas sin fallos después de integrar el starter Flyway requerido por Spring Boot 4.

### 2026-08-15 — contratos de zonas y mesas

- Se sustituyó la exposición directa de JPA por DTO de entrada/salida y una capa de servicios transaccional.
- Se agregaron filtros, consulta por id, creación y actualización para zonas y mesas.
- Se normalizan textos, se impiden nombres de zona y números de mesa duplicados y se exige una zona existente.
- Los errores de validación, inexistencia y conflicto usan respuestas REST uniformes con estados `400`, `404` y `409`.
- Verificación: `.\mvnw.cmd verify` finalizó con **13 pruebas, 0 fallos y 0 errores**.

### 2026-08-15 — autenticación, usuarios y roles

- Flyway V2 incorpora usuarios sin mezclar el concepto de seguridad con la futura entidad de negocio `Cliente`.
- Se implementaron los roles `ADMINISTRADOR`, `HOST`, `MESERO` y `CLIENTE`, contraseñas BCrypt, registro público limitado a cliente y login JWT HS256.
- Un administrador puede listar, filtrar, crear, actualizar y cambiar contraseñas de usuarios; ningún contrato expone hashes.
- Zonas y mesas permiten consulta a administrador/host/mesero y escritura sólo a administrador; cliente y anónimo reciben `403`/`401` JSON.
- Cada petición protegida vuelve a resolver en base de datos el estado y rol vigentes, por lo que los cambios de permisos tienen efecto inmediato.
- Verificación intermedia: `.\mvnw.cmd test` finalizó con **19 pruebas, 0 fallos y 0 errores**.

### 2026-08-15 — clientes y turnos

- Flyway V3 incorpora `clientes` y `turnos` con unicidad, relaciones, capacidad positiva, estado y marcas temporales.
- El registro público crea o vincula el perfil de negocio por correo sin mezclarlo con las credenciales; el cliente puede consultar y actualizar únicamente su perfil.
- Administrador y host pueden consultar, filtrar, crear y actualizar clientes; las respuestas nunca exponen la entidad JPA ni datos de contraseña.
- Los turnos admiten consulta y filtros para administrador/host/mesero, mientras la escritura queda limitada al administrador; se validan capacidad positiva y ventana horaria no vacía.
- Verificación: `.\mvnw.cmd test` finalizó con **27 pruebas, 0 fallos y 0 errores**.

### 2026-08-15 — disponibilidad y reservas

- Flyway V4 incorpora `reservas` e `historial_reservas`, estados contractuales, intervalos, versión optimista, relaciones, restricciones e índices operativos.
- La consulta de disponibilidad combina mesas activas, zona, capacidad individual, capacidad restante del turno y reservas que ocupan el intervalo.
- La creación bloquea turno y mesa antes de validar y persistir, impide superar capacidades y rechaza solapamientos de estados activos.
- Cliente crea/lista/cancela únicamente reservas propias; administrador/host registra reservas operativas y administrador/host/mesero consulta agenda y transiciona estados.
- Cada creación y transición registra estado anterior/nuevo, motivo, usuario y fecha; se probaron secuencia válida, estado final inmutable y autoría.
- Verificación final: `.\mvnw.cmd verify` finalizó con **35 pruebas, 0 fallos y 0 errores** y generó el JAR ejecutable.

### 2026-08-15 — documentación del Parcial 1

- Se consolidó el material inicial de visión, glosario y backlog sin modificar la carpeta original del Escritorio.
- `docs/README.md` indexa visión/contexto, actores, catálogo RF/RNF, RN-01 a RN-08, casos de uso/mapa, flujo crítico, modelo conceptual, DER, diccionario, arquitectura y backlog con aceptación.
- Los documentos distinguen alcance contractual, diseño futuro y evidencia realmente implementada.
- `AGENTS.md` conserva las reglas de trabajo y stack para futuras continuaciones.
- Verificación documental: **10 archivos Markdown**, más de **10 000 palabras**, **0 enlaces relativos rotos** y credencial administrativa restaurada a marcador seguro en `auth.http`.

### 2026-08-20 — check-in y reasignación trazable

- Flyway V5 amplía `historial_reservas` con tipo de evento, mesa anterior y mesa nueva, agrega sus claves foráneas/índices y restringe el estado operativo de mesa a `DISPONIBLE` u `OCUPADA`.
- `PATCH /api/reservas/{id}/check-in` permite a administrador/host pasar únicamente una reserva `CONFIRMADA` a `SENTADA`, conservando o cambiando mesa después de validar actividad, capacidad, disponibilidad operativa y solapamiento.
- `PATCH /api/reservas/{id}/mesa` reasigna reservas `CONFIRMADA` o `SENTADA`, exige motivo y conserva ambos extremos, actor y fecha en un evento append-only.
- Reserva y mesas se bloquean pesimistamente; las mesas se adquieren en orden de identificador para reducir interbloqueos durante operaciones concurrentes.
- El endpoint genérico ya no puede producir `SENTADA`; finalizar una visita sentada libera la mesa ocupada.
- Verificación final: `.\mvnw.cmd verify` finalizó con **38 pruebas, 0 fallos y 0 errores** y generó el JAR ejecutable.

### 2026-08-24 — auditoría y correcciones del backend actual

- Se corrigió la declaración dañada de `UsuarioController`, que impedía compilar el proyecto.
- RN-08 ahora exige el motivo de reasignación también dentro de `ReservaService`, además de la validación del contrato HTTP, para conservar la regla en la autoridad de negocio.
- Se agregó una prueba que rechaza motivos vacíos y comprueba que no cambien ni la mesa ni el historial.
- Se corrigieron contradicciones documentales sobre RN-08 y los estados operativos de mesa.
- Verificación final: `.\mvnw.cmd verify` finalizó con **39 pruebas, 0 fallos y 0 errores** y generó el JAR ejecutable.
