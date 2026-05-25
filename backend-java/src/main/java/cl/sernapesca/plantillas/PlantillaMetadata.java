package cl.sernapesca.plantillas;

import java.time.LocalDateTime;

/**
 * Representa la metadata de una plantilla mensual.
 * Usamos record de Java 21: inmutable, sin getters explícitos, sin boilerplate.
 */
public record PlantillaMetadata(
        String id,
        int anio,
        int mes,
        String tipo,           // RRA o RRA_FAR
        String nombreArchivo,
        long tamanoBytes,
        LocalDateTime fechaSubida
) {}
