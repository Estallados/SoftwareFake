# Matriz de trazabilidad

RF/RNF → historia → criterios de aceptación → clases → pruebas. Las pruebas llevan el ID del CA en el nombre (por ejemplo `debeRechazarSalidaIgualOAnteriorALaLlegada_CA122_CA082`).

| Módulo | RF / RNF | Historia | CA | Clases principales | Pruebas |
|---|---|---|---|---|---|
| 0. Base transversal | RNF-004, 008 a 014, 023 a 026 | SCRUM-48, 49, 50 | CA-131, CA-134 a CA-141, CA-149 a CA-153 | `AuditoriaService`, `AuditoriaRepository`, `EventoAuditoria`, `IdempotenciaService`, `RegistroIdempotencia`, `ManejadorGlobalExcepciones`, `ExceptionCheker`, `Resultado`, `RespuestaUtil`, `PaginaDTO` | `AuditoriaServiceTest`, `IdempotenciaServiceTest`, `ExceptionChekerTest`, `AdministracionTest#losRegistrosDeAuditoriaNoSePuedenModificarNiBorrar_CA150`, `ErrorResponseTest` |
| 1. Identidad y perfil | RF-001, 008, 009, 010, 011, 012 | SCRUM-33, 34 | CA-030 a CA-053 | `AuthController`, `UsuarioController`, `AuthService`, `UsuarioService`, `TokenRecuperacion`, `AdminInicializador` | `AuthFlowTest`, `IdentidadPerfilTest`, `AuthServiceTest`, `SeguridadRolesYCorsTest` |
| 2. Publicaciones | RF-017, 018, 019, 023, 027, RNF-005 | SCRUM-36, 40 | CA-084, 085, 093 a 104, 108, 109, 113, 114, 157, 158, 180, 185 a 187, 194 a 196 | `AlojamientoController`, `AdminController`, `PublicacionService`, `TarifaService`, `Alojamiento`, `HistorialEstadoAlojamiento`, `ConfiguracionTarifa`, `ServicioAdicional`, `Imagen` | `PublicacionesTest` |
| 3. Disponibilidad y búsqueda | RF-020, 024, 025, 026, RNF-015, 016, 027 | SCRUM-37, 39 | CA-076, 077, 087 a 092, 115 a 123, 190 | `DisponibilidadController`, `CalendarioService`, `PeriodoDisponibilidad`, `AlojamientoEspecificacion` | `DisponibilidadBusquedaTest`, `ReglasNegocioTest` |
| 4. Cotización, reserva y pago | RF-021, 022, 028, RNF-027 a 031 | SCRUM-40, 41 | CA-070 a 075, 078 a 083, 086, 140, 141, 159 a 163, 179, 181 a 184, 186 | `ReservaController`, `CotizacionService`, `ReservaService`, `PagoService`, `Cotizacion`, `Reserva`, `Pago` | `ReservasPagosTest`, `ReglasNegocioTest` |
| 5. Cancelación | RF-002 | SCRUM-42 | CA-001 a CA-009 | `ReservaController`, `CancelacionService`, `Cancelacion`, `PoliticaCancelacion`, `PoliticasInicializador` | `CancelacionesTest`, `ReglasNegocioTest` |
| 6. Notificaciones y reseñas | RF-003, 004 | SCRUM-43, 44 | CA-010, 013, 016 a 020 | `NotificacionController`, `ResenaController`, `NotificacionService`, `ResenaService`, `Notificacion`, `Resena` | `NotificacionesResenasTest` |
| 7. Administración | RF-013, 014, 015, 016 | SCRUM-35 | CA-054, 055, 058 a 060, 062, 064 a 067, 069, 111, 112, 137 a 139, 150 a 152 | `AdminController`, `PublicacionService`, `UsuarioService`, `ResenaService`, `AuditoriaService` | `AdministracionTest` |
| 8. Dashboard y reportes | RF-005, 006 | SCRUM-45 | CA-022 a CA-029 | `ReporteController`, `ReporteService`, `ExportadorUtil`, `RegistroExportacion` | `DashboardReportesTest` |
| 9. Opcionales | RF-007, 029, RNF-033 (RNF-032: solo latitud y longitud) | — | CA-165 a 178, CA-198 | `FavoritoController`, `MensajeriaController`, `SegundoFactorController`, `FavoritoService`, `MensajeriaService`, `SegundoFactorService`, `TotpUtil` | `OpcionalesTest`, `ReglasNegocioTest` |

## Criterios que no se prueban con JUnit

| CA | Motivo |
|---|---|
| CA-142, CA-143, CA-144, CA-145 (tiempos de respuesta) | Requieren pruebas de carga (por ejemplo JMeter). El back tiene índices en las columnas filtradas, paginación y la confirmación de reserva no retiene el bloqueo durante el pago. |
| CA-146, CA-147, CA-148 (diseño adaptable, formularios, estados visibles) | Son del front. El back entrega mensajes claros por campo y los estados de publicación y calendario. |
| CA-154, CA-155, CA-156 (disponibilidad, recuperación, persistencia) | Dependen del despliegue. El back expone `/actuator/health`, cierra de forma ordenada (`server.shutdown=graceful`) y todas las escrituras son transaccionales. |
| CA-164 (mapa) | Es del front; el back guarda latitud y longitud. |
