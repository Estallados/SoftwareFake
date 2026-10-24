package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.DashboardDTO;
import co.edu.unbosque.marketplace.dto.DemandaCiudadDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.Pago;
import co.edu.unbosque.marketplace.entity.RegistroExportacion;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.CancelacionRepository;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.repository.RegistroExportacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;
import co.edu.unbosque.marketplace.util.ArchivoReporte;
import co.edu.unbosque.marketplace.util.ExportadorUtil;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Dashboard administrativo y exportación de reportes en PDF o Excel. Cada
 * exportación queda en {@link RegistroExportacion} (quién y cuándo) y en la
 * auditoría.
 * <p>
 * Ingresos estimados = pagos aprobados o reembolsados − devoluciones
 * registradas en las cancelaciones.
 * </p>
 *
 * @see "RF-005, RF-006, CA-022 a CA-029"
 */
@Service
public class ReporteService {

    /** Reportes disponibles. */
    public static final List<String> TIPOS = List.of("usuarios", "alojamientos", "reservas", "ingresos", "demanda");

    /** Formatos disponibles. */
    public static final List<String> FORMATOS = List.of("pdf", "excel");

    private static final int CIUDADES_DASHBOARD = 5;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<EstadoPago> PAGOS_COBRADOS = List.of(EstadoPago.APROBADO, EstadoPago.REEMBOLSADO);

    private final UsuarioRepository usuarioRepo;
    private final AlojamientoRepository alojamientoRepo;
    private final ReservaRepository reservaRepo;
    private final PagoRepository pagoRepo;
    private final CancelacionRepository cancelacionRepo;
    private final RegistroExportacionRepository exportacionRepo;
    private final AuditoriaService auditoriaService;

    public ReporteService(UsuarioRepository usuarioRepo, AlojamientoRepository alojamientoRepo,
            ReservaRepository reservaRepo, PagoRepository pagoRepo, CancelacionRepository cancelacionRepo,
            RegistroExportacionRepository exportacionRepo, AuditoriaService auditoriaService) {
        this.usuarioRepo = usuarioRepo;
        this.alojamientoRepo = alojamientoRepo;
        this.reservaRepo = reservaRepo;
        this.pagoRepo = pagoRepo;
        this.cancelacionRepo = cancelacionRepo;
        this.exportacionRepo = exportacionRepo;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Indicadores del dashboard.
     *
     * @see "CA-022, CA-023, CA-024, CA-025, CA-026"
     */
    public DashboardDTO dashboard() {
        DashboardDTO dto = new DashboardDTO();
        dto.setTotalUsuarios(usuarioRepo.count());
        dto.setAlojamientosActivos(alojamientoRepo.countByEstado(EstadoAlojamiento.ACTIVA));

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoReserva estado : EstadoReserva.values()) {
            porEstado.put(estado.name(), 0L);
        }
        for (Object[] fila : reservaRepo.contarPorEstado()) {
            porEstado.put(fila[0].toString(), (Long) fila[1]);
        }
        dto.setReservasPorEstado(porEstado);
        dto.setIngresosEstimados(ingresosEstimados());
        dto.setCiudadesMayorDemanda(demanda().stream().limit(CIUDADES_DASHBOARD).toList());
        return dto;
    }

    /**
     * Exporta un reporte y registra quién lo pidió.
     *
     * @param tipo    usuarios, alojamientos, reservas, ingresos o demanda.
     * @param formato pdf o excel.
     * @return 0 con el archivo; 2 si el tipo o el formato no existen.
     * @see "CA-027, CA-028, CA-029"
     */
    @Transactional
    public Resultado<ArchivoReporte> exportar(String tipo, String formato, Long idActor) {
        String tipoNormalizado = tipo == null ? "" : tipo.trim().toLowerCase();
        String formatoNormalizado = formato == null ? "" : formato.trim().toLowerCase();
        if (!TIPOS.contains(tipoNormalizado) || !FORMATOS.contains(formatoNormalizado)) {
            return Resultado.error(Resultado.DATOS_INVALIDOS,
                    "Reporte no válido. Tipos: " + TIPOS + "; formatos: " + FORMATOS + ".");
        }

        List<String> encabezados = new ArrayList<>();
        List<List<String>> filas = new ArrayList<>();
        llenar(tipoNormalizado, encabezados, filas);

        String titulo = "Reporte de " + tipoNormalizado;
        boolean pdf = "pdf".equals(formatoNormalizado);
        byte[] contenido = pdf ? ExportadorUtil.pdf(titulo, encabezados, filas)
                : ExportadorUtil.excel(titulo, encabezados, filas);

        RegistroExportacion registro = new RegistroExportacion();
        registro.setTipoReporte(tipoNormalizado);
        registro.setFormato(formatoNormalizado);
        registro.setCantidadRegistros(filas.size());
        registro.setIdUsuario(idActor);
        registro.setFecha(LocalDateTime.now());
        registro = exportacionRepo.save(registro);
        auditoriaService.registrar(idActor, AuditoriaService.REPORTE_EXPORTADO, AuditoriaService.ENTIDAD_REPORTE,
                registro.getIdRegistro(), ResultadoAuditoria.EXITOSO,
                tipoNormalizado + " en " + formatoNormalizado + " (" + filas.size() + " registros)");

        String nombre = "reporte-" + tipoNormalizado + (pdf ? ".pdf" : ".xlsx");
        String tipoContenido = pdf ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return Resultado.exito(new ArchivoReporte(nombre, tipoContenido, contenido));
    }

    private void llenar(String tipo, List<String> encabezados, List<List<String>> filas) {
        switch (tipo) {
        case "usuarios":
            encabezados.addAll(List.of("Id", "Nombre", "Correo", "Rol", "Estado"));
            for (Usuario u : usuarioRepo.findAll()) {
                filas.add(List.of(texto(u.getIdUsuario()), AESUtil.decrypt(u.getNombre()),
                        AESUtil.decrypt(u.getCorreo()), texto(u.getRol()), texto(u.getEstado())));
            }
            break;
        case "alojamientos":
            encabezados.addAll(List.of("Id", "Título", "Ciudad", "Tipo", "Capacidad", "Precio noche", "Estado"));
            for (Alojamiento a : alojamientoRepo.findAll()) {
                filas.add(List.of(texto(a.getIdAlojamiento()), texto(a.getTitulo()), texto(a.getCiudad()),
                        texto(a.getTipo()), texto(a.getCapacidad()), texto(a.getPrecioNoche()), texto(a.getEstado())));
            }
            break;
        case "reservas":
            encabezados.addAll(List.of("Id", "Alojamiento", "Huésped", "Llegada", "Salida", "Estado", "Total"));
            for (Reserva r : reservaRepo.findAll()) {
                filas.add(List.of(texto(r.getIdReserva()), texto(r.getIdAlojamiento()), texto(r.getIdHuesped()),
                        texto(r.getFechaEntrada()), texto(r.getFechaSalida()), texto(r.getEstado()),
                        texto(r.getTotal())));
            }
            break;
        case "ingresos":
            encabezados.addAll(List.of("Pago", "Reserva", "Fecha", "Estado", "Valor"));
            for (Pago p : pagoRepo.findAll()) {
                filas.add(List.of(texto(p.getIdPago()), texto(p.getIdReserva()),
                        p.getFecha() == null ? "" : p.getFecha().format(FORMATO_FECHA), texto(p.getEstado()),
                        texto(p.getValor())));
            }
            filas.add(List.of("", "", "", "Ingresos estimados", texto(ingresosEstimados())));
            break;
        default:
            encabezados.addAll(List.of("Ciudad", "Reservas"));
            for (DemandaCiudadDTO demanda : demanda()) {
                filas.add(List.of(demanda.getCiudad(), texto(demanda.getReservas())));
            }
            break;
        }
    }

    private BigDecimal ingresosEstimados() {
        return pagoRepo.sumarValores(PAGOS_COBRADOS).subtract(cancelacionRepo.sumarReembolsos());
    }

    private List<DemandaCiudadDTO> demanda() {
        List<DemandaCiudadDTO> resultado = new ArrayList<>();
        for (Object[] fila : reservaRepo.contarPorCiudad()) {
            DemandaCiudadDTO demanda = new DemandaCiudadDTO();
            demanda.setCiudad(fila[0] == null ? "Sin ciudad" : fila[0].toString());
            demanda.setReservas((Long) fila[1]);
            resultado.add(demanda);
        }
        return resultado;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}
