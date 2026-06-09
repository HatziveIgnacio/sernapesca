package cl.sernapesca.reporte;

import cl.sernapesca.laboratorio.LaboratorioExcelService;
import cl.sernapesca.laboratorio.LaboratorioExcelService.FilaConDatos;
import cl.sernapesca.periodo.EstadoPeriodo;
import cl.sernapesca.periodo.PeriodoReporte;
import cl.sernapesca.periodo.PeriodoReporteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Finaliza un reporte de laboratorio: lo valida una última vez y, si no tiene
 * errores, persiste sus registros en BD para que el consolidado del período
 * pueda apilarlos después.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final LaboratorioExcelService excelService;
    private final ReporteRepository reporteRepo;
    private final RegistroReporteRepository registroRepo;
    private final PeriodoReporteRepository periodoRepo;
    private final ObjectMapper objectMapper;

    public record FinalizarResumen(Long reporteId, String tipo, Integer anio, Integer mes,
                                   String nombreLaboratorio, int totalRegistros) {}

    @Transactional
    public FinalizarResumen finalizar(MultipartFile file, String tipo, Integer anio, Integer mes) throws Exception {
        List<FilaConDatos> filas = excelService.parsearYValidar(file, tipo, anio, mes);

        if (filas.isEmpty()) {
            throw new IllegalArgumentException(
                "El archivo no contiene registros. Cargue datos en la hoja 'ingreso' antes de finalizar.");
        }

        long conError = filas.stream().filter(f -> !f.validacion().isValid()).count();
        if (conError > 0) {
            throw new IllegalStateException(
                "El reporte tiene " + conError + " registro(s) con error. Corrija los errores antes de finalizar y subir.");
        }

        // Resolver el período: por año/mes si vienen, o el ABIERTO más reciente del tipo.
        PeriodoReporte periodo = (anio != null && mes != null)
            ? periodoRepo.findByAnioAndMesAndTipo(anio, mes, tipo).orElse(null)
            : periodoRepo.findFirstByTipoAndEstadoOrderByAnioDescMesDesc(tipo, EstadoPeriodo.ABIERTO).orElse(null);

        if (periodo == null) {
            throw new IllegalStateException(
                "No hay un período abierto para " + tipo + ". SERNAPESCA debe crear el período antes de recibir reportes.");
        }

        String nombreLab = extraerNombreLaboratorio(filas);

        Reporte reporte = reporteRepo.save(Reporte.builder()
                .periodoId(periodo.getId())
                .tipo(tipo)
                .anio(periodo.getAnio())
                .mes(periodo.getMes())
                .nombreLaboratorio(nombreLab)
                .nombreArchivo(file.getOriginalFilename())
                .fechaEnvio(LocalDateTime.now())
                .totalRegistros(filas.size())
                .build());

        List<RegistroReporte> registros = new ArrayList<>(filas.size());
        for (FilaConDatos f : filas) {
            registros.add(RegistroReporte.builder()
                    .reporteId(reporte.getId())
                    .numeroFila(f.numeroFila())
                    .datosJson(objectMapper.writeValueAsString(f.datos()))
                    .build());
        }
        registroRepo.saveAll(registros);

        log.info("Reporte finalizado: id={} lab='{}' {}/{} tipo={} registros={}",
                reporte.getId(), nombreLab, periodo.getMes(), periodo.getAnio(), tipo, filas.size());

        return new FinalizarResumen(reporte.getId(), tipo, periodo.getAnio(), periodo.getMes(), nombreLab, filas.size());
    }

    private String extraerNombreLaboratorio(List<FilaConDatos> filas) {
        return filas.stream()
                .map(f -> f.datos().get("nombre_laboratorio"))
                .filter(v -> v != null && !v.toString().isBlank())
                .map(Object::toString)
                .findFirst()
                .orElse("Desconocido");
    }
}
