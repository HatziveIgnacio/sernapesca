package cl.sernapesca.laboratorio.validators;

import org.springframework.stereotype.Service;
import cl.sernapesca.laboratorio.validators.ValidationTypes.FieldError;
import cl.sernapesca.laboratorio.validators.ValidationTypes.ValidatedRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RraValidatorService {

    private static final DateTimeFormatter CL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> VERIFICACION_TYPES = Set.of("verificación sernapesca", "verificación periódica pac");
    private static final String FEMPAC = "FEMPAC";

    // NOTA: el documento menciona "fecha muestreo > inicio quincena", pero los datos
    // reales de RRA muestran muestreos repartidos por todo el mes (38% en días 1-15),
    // así que NO se valida como día fijo. La regla real depende de qué quincena
    // reporta cada archivo, dato que aún no se modela. PENDIENTE: confirmar con SERNAPESCA.

    private boolean isEmpty(Object v) {
        return v == null || v.toString().trim().isEmpty();
    }

    /** ¿El tipo de control es de verificación? Comparación case-insensitive. */
    private boolean esVerificacion(String tipoControl) {
        return tipoControl != null && VERIFICACION_TYPES.contains(tipoControl.trim().toLowerCase());
    }

    private FieldError err(String col, String field, String msg) {
        return new FieldError(col, field, msg);
    }

    /**
     * Valida un campo contra el catálogo dinámico del período. Solo marca error
     * si el campo tiene catálogo cargado, el valor no está vacío y no pertenece
     * al catálogo. Campos sin catálogo (período sin plantilla) no se bloquean.
     */
    private void validarCatalogo(Map<String, Object> row, String field, String col,
                                 ReglasDinamicas reglas, List<FieldError> errors) {
        Object valor = row.get(field);
        if (isEmpty(valor)) return; // la obligatoriedad se valida aparte
        if (reglas.tieneCatalogo(field) && !reglas.permite(field, valor)) {
            String v = valor.toString().trim();
            if (v.length() > 60) v = v.substring(0, 60) + "…";
            errors.add(err(col, field, "El valor '" + v + "' no está en la lista de la plantilla del período"));
        }
    }

    /**
     * Valida una fecha obligatoria que forma parte de la cadena cronológica:
     * comprueba obligatoriedad, formato, rango de 4 años, y que sea mayor o igual
     * a la fecha previa de la cadena (si esa existe).
     */
    private LocalDate validarFechaCadena(Map<String, Object> row, String field, String col,
                                         LocalDate previa, String nombrePrevia,
                                         LocalDate min, LocalDate max, List<FieldError> errors) {
        if (isEmpty(row.get(field))) {
            errors.add(err(col, field, "Campo obligatorio"));
            return null;
        }
        LocalDate d = validateDateRange(row.get(field), col, field, min, max, errors);
        if (d != null && previa != null && d.isBefore(previa)) {
            errors.add(err(col, field, "Debe ser mayor o igual a la " + nombrePrevia));
        }
        return d;
    }

    private LocalDate validateDateRange(Object value, String col, String field, LocalDate min, LocalDate max, List<FieldError> errors) {
        LocalDate d = DateUtils.parseDate(value);
        if (d == null) {
            errors.add(err(col, field, "Fecha inválida. Use formato dd/mm/aaaa"));
            return null;
        }
        if (d.isBefore(min)) errors.add(err(col, field, "Fecha anterior al mínimo permitido (" + min.format(CL_DATE_FORMAT) + ")"));
        if (d.isAfter(max)) errors.add(err(col, field, "Fecha posterior al máximo permitido (" + max.format(CL_DATE_FORMAT) + ")"));
        return d;
    }

    public ValidatedRow validate(Map<String, Object> row, int rowNumber, ReglasDinamicas reglas) {
        List<FieldError> errors = new ArrayList<>();
        LocalDate now = DateUtils.today();
        LocalDate minDate = DateUtils.addYears(now, -4);
        LocalDate maxDate = now;
        LocalDate maxDatePlus1Y = DateUtils.addYears(now, 1);

        // Campos obligatorios fijos
        Map<String, String> requiredFields = Map.of(
            "nombre_laboratorio", "A", "tipo_laboratorio", "B", 
            "n_informe", "C", "n_formulario", "E"
        );
        for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
            if (isEmpty(row.get(entry.getKey()))) errors.add(err(entry.getValue(), entry.getKey(), "Campo obligatorio"));
        }

        // Tipo Formulario — obligatoriedad + catálogo dinámico
        String tipoFormulario = isEmpty(row.get("tipo_formulario")) ? "" : row.get("tipo_formulario").toString().trim();
        if (tipoFormulario.isEmpty()) {
            errors.add(err("D", "tipo_formulario", "Campo obligatorio"));
        } else {
            validarCatalogo(row, "tipo_formulario", "D", reglas, errors);
        }

        // Tipo Control — obligatoriedad + catálogo dinámico
        String tipoControl = isEmpty(row.get("tipo_control")) ? "" : row.get("tipo_control").toString().trim();
        if (tipoControl.isEmpty()) {
            errors.add(err("F", "tipo_control", "Campo obligatorio"));
        } else {
            validarCatalogo(row, "tipo_control", "F", reglas, errors);
        }

        // Lógica de negocio asociada a valores conocidos (se mantiene aunque el catálogo crezca)
        if (FEMPAC.equalsIgnoreCase(tipoFormulario) && isEmpty(row.get("id_siscomex"))) {
            errors.add(err("G", "id_siscomex", "Obligatorio cuando el formulario es FEMPAC"));
        }

        if (isEmpty(row.get("codigo_establecimiento"))) errors.add(err("H", "codigo_establecimiento", "Campo obligatorio"));
        if (isEmpty(row.get("razon_social"))) errors.add(err("I", "razon_social", "Campo obligatorio"));

        // Catálogos dinámicos para el resto de campos mapeados (solo si tienen lista cargada)
        validarCatalogo(row, "tipo_laboratorio", "B", reglas, errors);
        validarCatalogo(row, "nombre_laboratorio", "A", reglas, errors);
        validarCatalogo(row, "id_siscomex", "G", reglas, errors);
        validarCatalogo(row, "codigo_establecimiento", "H", reglas, errors);
        validarCatalogo(row, "codigo_area_extraccion", "J", reglas, errors);
        validarCatalogo(row, "tipo_consumo", "L", reglas, errors);
        validarCatalogo(row, "codigo_producto", "M", reglas, errors);
        validarCatalogo(row, "nombre_entidad_muestreo", "S", reglas, errors);
        validarCatalogo(row, "tipo_analisis", "X", reglas, errors);
        validarCatalogo(row, "analisis_solicitado", "Y", reglas, errors);
        validarCatalogo(row, "unidad_medida", "AA", reglas, errors);

        // Fechas y lógica cruzada
        String codigoArea = isEmpty(row.get("codigo_area_extraccion")) ? "" : row.get("codigo_area_extraccion").toString().trim();
        LocalDate fechaExtraccion = null;
        if (!codigoArea.isEmpty()) {
            if (isEmpty(row.get("fecha_extraccion"))) {
                errors.add(err("K", "fecha_extraccion", "Obligatorio cuando se indica código de área"));
            } else {
                fechaExtraccion = validateDateRange(row.get("fecha_extraccion"), "K", "fecha_extraccion", minDate, maxDate, errors);
            }
        }

        if (isEmpty(row.get("tipo_consumo"))) errors.add(err("L", "tipo_consumo", "Campo obligatorio"));
        if (isEmpty(row.get("codigo_producto"))) errors.add(err("M", "codigo_producto", "Campo obligatorio"));
        if (isEmpty(row.get("nombre_comun"))) errors.add(err("N", "nombre_comun", "Campo obligatorio"));
        if (isEmpty(row.get("linea_proceso"))) errors.add(err("O", "linea_proceso", "Campo obligatorio"));

        LocalDate fechaElaboracion = null;
        if (isEmpty(row.get("fecha_elaboracion"))) {
            errors.add(err("P", "fecha_elaboracion", "Campo obligatorio"));
        } else {
            fechaElaboracion = validateDateRange(row.get("fecha_elaboracion"), "P", "fecha_elaboracion", minDate, maxDate, errors);
            if (fechaElaboracion != null && fechaExtraccion != null && !fechaElaboracion.isAfter(fechaExtraccion)) {
                errors.add(err("P", "fecha_elaboracion", "Debe ser posterior a la fecha de extracción"));
            }
        }

        LocalDate fechaInicioVer = null;
        if (esVerificacion(tipoControl)) {
            if (isEmpty(row.get("fecha_inicio_verificacion"))) {
                errors.add(err("Q", "fecha_inicio_verificacion", "Obligatorio para Verificación Sernapesca/Periódica"));
            } else {
                fechaInicioVer = validateDateRange(row.get("fecha_inicio_verificacion"), "Q", "fecha_inicio_verificacion", minDate, maxDate, errors);
                if (fechaInicioVer != null && fechaElaboracion != null && fechaInicioVer.isAfter(fechaElaboracion)) {
                    errors.add(err("Q", "fecha_inicio_verificacion", "Debe ser menor o igual a fecha de elaboración"));
                }
            }
        }

        LocalDate fechaFinVer = null;
        if (esVerificacion(tipoControl)) {
            if (isEmpty(row.get("fecha_fin_verificacion"))) {
                errors.add(err("R", "fecha_fin_verificacion", "Obligatorio para Verificación Sernapesca/Periódica"));
            } else {
                fechaFinVer = validateDateRange(row.get("fecha_fin_verificacion"), "R", "fecha_fin_verificacion", minDate, maxDatePlus1Y, errors);
                if (fechaFinVer != null && fechaInicioVer != null && fechaFinVer.isBefore(fechaInicioVer)) {
                    errors.add(err("R", "fecha_fin_verificacion", "Debe ser mayor o igual a fecha de inicio de verificación"));
                }
                if (fechaFinVer != null && fechaElaboracion != null && fechaFinVer.isBefore(fechaElaboracion)) {
                    errors.add(err("R", "fecha_fin_verificacion", "Debe ser mayor o igual a la fecha de elaboración"));
                }
            }
        }

        if (isEmpty(row.get("nombre_entidad_muestreo"))) errors.add(err("S", "nombre_entidad_muestreo", "Campo obligatorio"));

        LocalDate fechaMuestreo = null;
        if (isEmpty(row.get("fecha_muestreo"))) {
            errors.add(err("T", "fecha_muestreo", "Campo obligatorio"));
        } else {
            fechaMuestreo = validateDateRange(row.get("fecha_muestreo"), "T", "fecha_muestreo", minDate, maxDate, errors);
            if (fechaMuestreo != null && fechaElaboracion != null && fechaMuestreo.isBefore(fechaElaboracion)) {
                errors.add(err("T", "fecha_muestreo", "Debe ser mayor a la fecha de elaboración"));
            }
        }

        // Campos finales no-fecha: obligatorios (los catálogos ya se validaron arriba)
        Map<String, String> obligNoFecha = Map.of(
            "codigo_muestra", "W", "tipo_analisis", "X", "analisis_solicitado", "Y",
            "valor_obtenido", "Z", "unidad_medida", "AA"
        );
        for (Map.Entry<String, String> entry : obligNoFecha.entrySet()) {
            if (isEmpty(row.get(entry.getKey()))) errors.add(err(entry.getValue(), entry.getKey(), "Campo obligatorio"));
        }

        // Cadena cronológica de fechas: envío ≥ muestreo ≥ ... ≥ emisión (cada una con rango 4 años)
        LocalDate fechaEnvio = validarFechaCadena(row, "fecha_envio_muestras", "U", fechaMuestreo, "fecha de muestreo", minDate, maxDate, errors);
        LocalDate fechaRecepcion = validarFechaCadena(row, "fecha_recepcion_muestras", "V", fechaEnvio, "fecha de envío de muestras", minDate, maxDate, errors);
        LocalDate fechaInicioAnalisis = validarFechaCadena(row, "fecha_inicio_analisis", "AB", fechaRecepcion, "fecha de recepción de muestras", minDate, maxDate, errors);
        LocalDate fechaObtencion = validarFechaCadena(row, "fecha_obtencion_resultados", "AC", fechaInicioAnalisis, "fecha de inicio de análisis", minDate, maxDate, errors);
        validarFechaCadena(row, "fecha_emision_informe", "AD", fechaObtencion, "fecha de obtención de resultados", minDate, maxDate, errors);

        // AF: nombre de laboratorio de externalización obligatorio si se indicó externalización
        if (!isEmpty(row.get("externalizacion")) && isEmpty(row.get("nombre_lab_externalizacion"))) {
            errors.add(err("AF", "nombre_lab_externalizacion", "Obligatorio cuando se indica externalización"));
        }

        return new ValidatedRow(rowNumber, errors.isEmpty(), DateUtils.buildDisplay(row), errors);
    }
}