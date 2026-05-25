package cl.sernapesca.plantillas;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Lógica de negocio para las plantillas mensuales.
 *
 * Por ahora guarda archivos en disco y mantiene metadata en memoria.
 * Cuando la BD esté lista (módulo de Maximiliano), este service
 * se conecta al repositorio JPA sin cambiar el controller.
 */
@Slf4j
@Service
public class PlantillaService {

    private final Path storageDir;

    // Lista en memoria mientras no hay BD. Se reemplaza por JPA después.
    private final List<PlantillaMetadata> plantillas = new ArrayList<>();

    public PlantillaService(@Value("${app.storage.path:./storage}") String storagePath) throws IOException {
        this.storageDir = Paths.get(storagePath, "plantillas");
        Files.createDirectories(this.storageDir);
        log.info("Directorio de plantillas: {}", this.storageDir.toAbsolutePath());
    }

    /**
     * Guarda la plantilla Excel mensual en disco y registra su metadata.
     *
     * @param file       archivo Excel subido por SERNAPESCA
     * @param anio       año del período (ej: 2026)
     * @param mes        mes del período (1-12)
     * @param tipo       tipo de plantilla: RRA o RRA_FAR
     */
    public PlantillaMetadata guardar(MultipartFile file, int anio, int mes, String tipo) throws IOException {
        validarTipo(tipo);
        validarMes(mes);
        validarPeriodoNoFuturo(anio, mes);

        String nombreArchivo = String.format("plantilla_%s_%d_%02d_%s",
                tipo.toLowerCase(), anio, mes, file.getOriginalFilename());
        Path destino = storageDir.resolve(nombreArchivo);

        Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        log.info("Plantilla guardada: {}", destino);

        PlantillaMetadata meta = new PlantillaMetadata(
                UUID.randomUUID().toString(),
                anio, mes, tipo,
                nombreArchivo,
                file.getSize(),
                LocalDateTime.now()
        );
        // Reemplaza si ya existía una para el mismo período y tipo
        plantillas.removeIf(p -> p.anio() == anio && p.mes() == mes && p.tipo().equals(tipo));
        plantillas.add(meta);
        return meta;
    }

    /**
     * Lista todas las plantillas registradas, ordenadas por año/mes descendente.
     */
    public List<PlantillaMetadata> listar() {
        return plantillas.stream()
                .sorted(Comparator.comparing(PlantillaMetadata::anio)
                        .thenComparing(PlantillaMetadata::mes).reversed())
                .toList();
    }

    /**
     * Retorna el path en disco de una plantilla dado su ID.
     */
    public Optional<Path> obtenerArchivo(String id) {
        return plantillas.stream()
                .filter(p -> p.id().equals(id))
                .map(p -> storageDir.resolve(p.nombreArchivo()))
                .findFirst();
    }

    public Optional<PlantillaMetadata> obtenerMetadata(String id) {
        return plantillas.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /**
     * Retorna la plantilla más reciente de un tipo dado (RRA o RRA_FAR).
     * "Más reciente" = mayor anio, luego mayor mes.
     */
    public Optional<PlantillaMetadata> obtenerUltima(String tipo) {
        return plantillas.stream()
                .filter(p -> p.tipo().equals(tipo))
                .max(Comparator.comparingInt(PlantillaMetadata::anio)
                        .thenComparingInt(PlantillaMetadata::mes));
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

    private void validarTipo(String tipo) {
        if (!Set.of("RRA", "RRA_FAR").contains(tipo)) {
            throw new IllegalArgumentException("Tipo de plantilla inválido: " + tipo + ". Use RRA o RRA_FAR");
        }
    }

    private void validarMes(int mes) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mes inválido: " + mes + ". Debe ser entre 1 y 12");
        }
    }

    private void validarPeriodoNoFuturo(int anio, int mes) {
        LocalDate hoy = LocalDate.now();
        int anioActual = hoy.getYear();
        int mesActual  = hoy.getMonthValue();
        if (anio > anioActual || (anio == anioActual && mes > mesActual)) {
            throw new IllegalArgumentException(
                String.format("No se puede subir una plantilla para un período futuro (%02d/%d). " +
                              "El período máximo permitido es %02d/%d.",
                              mes, anio, mesActual, anioActual));
        }
    }
}
