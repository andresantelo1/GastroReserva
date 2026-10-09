# GastroReserva — visión y contexto del cliente

## Identificación

| Campo | Definición |
|---|---|
| Proyecto | 03. GastroReserva — Reservas, Mesas, Pedidos y Experiencia de Restaurante |
| Código | PA-03 |
| Asignatura | Programación Aplicada 2026-2 |
| Tipo | Proyecto integrador full-stack web + móvil |
| Cliente objetivo | Restaurante de tamaño medio |
| Stack obligatorio | Java 21, Spring Boot, PostgreSQL, React + TypeScript, React Native + TypeScript, Docker y GitHub Actions |

## Contexto

El restaurante gestiona reservas, disponibilidad de mesas, turnos y pedidos mediante llamadas, papel y mensajería. La información queda distribuida entre distintos medios, lo que dificulta conocer la disponibilidad real, coordinar al personal de salón y reconstruir lo ocurrido durante una visita.

## Problema

La operación manual produce tres problemas principales:

1. Riesgo de reservas solapadas o aceptadas sin capacidad suficiente.
2. Falta de una vista única para clientes, mesas, turnos, pedidos y estados de atención.
3. Poca trazabilidad desde la reserva hasta la finalización de la visita y el feedback.

## Visión del producto

GastroReserva será una plataforma web y móvil que centralizará el ciclo operativo de una visita al restaurante. La experiencia web estará orientada a administración, recepción y operación de mayor densidad de información. La experiencia móvil priorizará el autoservicio del cliente y las acciones rápidas del personal de salón.

## Objetivo general

Desarrollar un sistema que gestione reservas y la operación básica de salón, permitiendo controlar capacidad, mesas, clientes, pedidos, estados y retroalimentación sin convertirse en un sistema contable.

## Objetivos específicos

- Autenticar usuarios y aplicar permisos según su rol vigente.
- Mantener clientes, zonas, mesas, turnos y capacidades.
- Consultar disponibilidad y crear reservas sin exceder capacidad ni solapar mesas.
- Registrar check-in, asignación y reasignación trazable de mesas.
- Mantener una carta básica y pedidos simples con total operativo.
- Conservar el precio histórico de los ítems de un pedido.
- Finalizar la visita y permitir feedback únicamente después de dicha finalización.
- Producir indicadores de ocupación y no-show.
- Clasificar comentarios mediante una integración de IA opcional y no bloqueante.

## Alcance cerrado del MVP

- Autenticación y roles.
- Clientes.
- Zonas y mesas.
- Turnos y capacidad.
- Disponibilidad y reservas.
- Check-in, asignación y reasignación.
- Carta básica.
- Pedidos y estados de atención.
- Feedback.
- Panel de ocupación y reportes de no-show.
- Experiencia web administrativa y operativa.
- Experiencia móvil para cliente y mesero.

## Exclusiones

El MVP no incluye contabilidad, facturación fiscal, métodos de pago ni funcionalidades propias de un sistema contable. El pedido sólo calcula un total operativo del consumo.

## Flujo crítico y criterio de éxito

El producto debe demostrar de extremo a extremo el siguiente flujo:

```text
Cliente consulta horario → reserva → host realiza check-in y asigna mesa
→ mesero gestiona pedido → se finaliza la visita → cliente registra feedback
```

El flujo será exitoso cuando use el mismo backend desde las experiencias correspondientes, respete permisos y reglas de negocio, persista los cambios y presente evidencia de resultados correctos y errores controlados.

## Estado real al 2026-10-08

El backend principal incluye autenticación, clientes, salón, turnos, reservas, check-in, reasignación, carta, mesa abierta, pedidos y atención, feedback y reportes. Flyway mantiene ocho migraciones y existen pruebas de servicio, permisos HTTP y concurrencia. IA, Docker y GitHub Actions se aplazan por decisión del usuario; las experiencias web/móvil siguen pendientes. La evidencia y las limitaciones se registran en la matriz de estado.

La cobertura exacta y sus evidencias se mantienen en [PA-03-status.md](PA-03-status.md).

## Glosario

| Término | Definición |
|---|---|
| Cliente | Persona que realiza una reserva o recibe atención en el restaurante. |
| Usuario | Identidad de acceso autenticada, separada del perfil de negocio del cliente. |
| Zona | Sector físico del restaurante que agrupa mesas. |
| Mesa | Espacio físico con una capacidad determinada. |
| Turno | Intervalo de atención con horarios y capacidad máxima definidos. |
| Reserva | Solicitud de atención para una fecha, turno, mesa y cantidad de personas. |
| Capacidad | Cantidad máxima de personas que una mesa o turno puede admitir. |
| Solapamiento | Conflicto producido cuando una mesa posee reservas activas coincidentes en un intervalo. |
| Check-in | Registro de la llegada del cliente con reserva. |
| Asignación | Asociación operativa de una mesa disponible con una reserva. |
| Reasignación | Cambio de la mesa asignada, conservando trazabilidad. |
| Carta | Catálogo básico de productos ofrecidos por el restaurante. |
| ProductoMenu | Producto de la carta con nombre, precio y disponibilidad. |
| Pedido | Conjunto de productos solicitados durante una visita. |
| ItemPedido | Producto, cantidad y precio histórico pertenecientes a un pedido. |
| EstadoPedido | Situación actual del pedido durante su atención. |
| HistorialReserva | Registro de creación, cambios de estado, check-in y reasignaciones de una reserva. |
| No-show | Reserva confirmada cuyo cliente no se presentó. |
| Feedback | Valoración o comentario posterior a una visita finalizada. |
| Ocupación | Indicador de utilización de mesas o capacidad disponible. |
