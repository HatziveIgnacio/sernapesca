package cl.sernapesca.periodo;

import cl.sernapesca.laboratorio.validators.PlantillaRulesExtractorService;
import cl.sernapesca.periodo.dto.CrearPeriodoRequest;
import cl.sernapesca.plantillas.PlantillaMetadata;
import cl.sernapesca.plantillas.PlantillaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodoReporteService {

    private static final Set<String> TIPOS_VALIDOS = Set.of("RRA", "RRA_FAR");

    private final PeriodoReporteRepository repository;
    private final PlantillaService plantillaService;
    private final PlantillaRulesExtractorService rulesExtractor;

    @Transactional(readOnly = true)
    public List<PeriodoReporte> listar() {
        return repository.findAllByOrderByAnioDescMesDescTipoAsc();
    }

    @Transactional(readOnly = true)
    public PeriodoReporte obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Período no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public PeriodoReporte obtenerActivoPorTipo(String tipo) {
        validarTipo(tipo);
        return repository.findFirstByTipoAndEstadoOrderByAnioDescMesDesc(tipo, EstadoPeriodo.ABIERTO)
                .orElseThrow(() -> new NoSuchElementException(
                        "No hay período ABIERTO para tipo " + tipo));
    }

    @Transactional
    public PeriodoReporte crear(CrearPeriodoRequest req) {
        validarTipo(req.getTipo());
        validarPeriodoNoFuturo(req.getAnio(), req.getMes());

        repository.findByAnioAndMesAndTipo(req.getAnio(), req.getMes(), req.getTipo())
                .ifPresent(p -> {
                    throw new IllegalStateException(String.format(
                        "Ya existe un período %s para %02d/%d", req.getTipo(), req.getMes(), req.getAnio()));
                });

        if (req.getPlantillaId() != null && !req.getPlantillaId().isBlank()) {
            validarPlantilla(req.getPlantillaId(), req.getTipo());
        }

        PeriodoReporte nuevo = PeriodoReporte.builder()
                .anio(req.getAnio())
                .mes(req.getMes())
                .tipo(req.getTipo())
                .plantillaId(req.getPlantillaId())
                .estado(EstadoPeriodo.ABIERTO)
                .fechaCreacion(LocalDateTime.now())
                .consolidadoSolicitado(false)
                .build();

        PeriodoReporte guardado = repository.save(nuevo);
        log.info("Período creado: id={} {}/{} tipo={}", guardado.getId(),
                guardado.getMes(), guardado.getAnio(), guardado.getTipo());

        if (guardado.getPlantillaId() != null && !guardado.getPlantillaId().isBlank()) {
            extraerReglasSeguro(guardado);
        }
        return guardado;
    }

    @Transactional
    public PeriodoReporte cerrar(Long id) {
        PeriodoReporte p = obtener(id);
        if (p.getEstado() == EstadoPeriodo.CERRADO) {
            throw new IllegalStateException("El período ya está cerrado (id=" + id + ")");
        }

        p.setEstado(EstadoPeriodo.CERRADO);
        p.setFechaCierre(LocalDateTime.now());
        p.setConsolidadoSolicitado(true);

        log.info("Período CERRADO: id={} {}/{} tipo={} → señal de consolidado emitida",
                p.getId(), p.getMes(), p.getAnio(), p.getTipo());

        // TODO: cuando exista el módulo de consolidados, gatillar aquí su generación
        // (publicación de evento Spring o llamada directa al ConsolidadoService).

        return repository.save(p);
    }

    @Transactional
    public PeriodoReporte vincularPlantilla(Long id, String plantillaId) {
        PeriodoReporte p = obtener(id);
        if (p.getEstado() == EstadoPeriodo.CERRADO) {
            throw new IllegalStateException("No se puede modificar un período cerrado");
        }
        validarPlantilla(plantillaId, p.getTipo());
        p.setPlantillaId(plantillaId);
        PeriodoReporte guardado = repository.save(p);
        extraerReglasSeguro(guardado);
        return guardado;
    }

    /**
     * Extracción manual de reglas (endpoint POST /api/periodos/{id}/extraer-reglas).
     * Propaga errores para que el usuario sepa si falló.
     */
    @Transactional
    public PlantillaRulesExtractorService.ExtraccionResumen extraerReglas(Long id) {
        PeriodoReporte p = obtener(id);
        if (p.getPlantillaId() == null || p.getPlantillaId().isBlank()) {
            throw new IllegalStateException("El período no tiene plantilla vinculada");
        }
        Path archivo = resolverArchivoPlantilla(p);
        try {
            return rulesExtractor.extraerYGuardar(archivo, p.getId());
        } catch (Exception e) {
            throw new RuntimeException("Error extrayendo reglas de la plantilla: " + e.getMessage(), e);
        }
    }

    // ── Helpers de extracción ──────────────────────────────────────────────

    /** Gatillo automático: loguea el error sin abortar el alta/vinculación del período. */
    private void extraerReglasSeguro(PeriodoReporte p) {
        try {
            Path archivo = resolverArchivoPlantilla(p);
            var resumen = rulesExtractor.extraerYGuardar(archivo, p.getId());
            log.info("Reglas extraídas para período {}: {} valores", p.getId(), resumen.totalValores());
        } catch (Exception e) {
            log.warn("No se pudieron extraer reglas del período {} (se puede reintentar manualmente): {}",
                    p.getId(), e.getMessage());
        }
    }

    private Path resolverArchivoPlantilla(PeriodoReporte p) {
        Optional<Path> archivo = plantillaService.obtenerArchivo(p.getPlantillaId());
        return archivo.orElseThrow(() -> new NoSuchElementException(
                "No se encontró el archivo de la plantilla " + p.getPlantillaId()));
    }

    // ── Validaciones internas ──────────────────────────────────────────────

    private void validarTipo(String tipo) {
        if (tipo == null || !TIPOS_VALIDOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo inválido: " + tipo + ". Use RRA o RRA_FAR");
        }
    }

    private void validarPeriodoNoFuturo(int anio, int mes) {
        LocalDate hoy = LocalDate.now();
        if (anio > hoy.getYear() || (anio == hoy.getYear() && mes > hoy.getMonthValue())) {
            throw new IllegalArgumentException(String.format(
                "No se puede crear un período futuro (%02d/%d). Máximo permitido: %02d/%d",
                mes, anio, hoy.getMonthValue(), hoy.getYear()));
        }
    }

    private void validarPlantilla(String plantillaId, String tipoPeriodo) {
        PlantillaMetadata meta = plantillaService.obtenerMetadata(plantillaId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Plantilla no encontrada: " + plantillaId));
        if (!meta.tipo().equals(tipoPeriodo)) {
            throw new IllegalArgumentException(String.format(
                "El tipo de la plantilla (%s) no coincide con el del período (%s)",
                meta.tipo(), tipoPeriodo));
        }
    }
}
