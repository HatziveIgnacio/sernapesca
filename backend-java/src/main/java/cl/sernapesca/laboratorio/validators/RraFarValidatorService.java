package cl.sernapesca.laboratorio.validators;

import org.springframework.stereotype.Service;
import cl.sernapesca.laboratorio.validators.ValidationTypes.FieldError;
import cl.sernapesca.laboratorio.validators.ValidationTypes.ValidatedRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RraFarValidatorService {

    private static final DateTimeFormatter CL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> VERIFICACION_TYPES = Set.of("verificación sernapesca", "verificación periódica pac");
    private static final Set<String> CENTRO_CULTIVO_CONTROLS = Set.of("Control Mensual", "Control de Sustancias Prohibidas y No autorizadas", "Control Precosecha");
    private static final Set<String> FEMPAC_SMAE = Set.of("FEMPAC", "SMAE");

    // NOTA: regla "fecha muestreo > inicio quincena" pendiente. Ver RraValidatorService:
    // los datos reales de RRA muestran muestreos repartidos por todo el mes, así que no
    // se valida como día fijo. PENDIENTE: confirmar definición con SERNAPESCA.

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
        if (isEmpty(valor)) return;
        if (reglas.tieneCatalogo(field) && !reglas.permite(field, valor)) {
            String v = valor.toString().trim();
            if (v.length() > 60) v = v.substring(0, 60) + "…";
            errors.add(err(col, field, "El valor '" + v + "' no está en la lista de la plantilla del período"));
        }
    }

    /**
     * Valida una fecha obligatoria de la cadena cronológica: obligatoriedad,
     * formato, rango de 4 años y que sea mayor o igual a la fecha previa.
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

        // A, B, C, E — Obligatorios
        Map<String, String> requiredFields = Map.of(
            "nombre_laboratorio", "A", "tipo_laboratorio", "B",
            "n_informe", "C", "n_formulario", "E"
        );
        for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
            if (isEmpty(row.get(entry.getKey()))) errors.add(err(entry.getValue(), entry.getKey(), "Campo obligatorio"));
        }

        // Catálogos dinámicos (columnas propias de RRA FAR)
        validarCatalogo(row, "nombre_laboratorio", "A", reglas, errors);
        validarCatalogo(row, "tipo_laboratorio", "B", reglas, errors);
        validarCatalogo(row, "tipo_formulario", "D", reglas, errors);
        validarCatalogo(row, "tipo_control", "F", reglas, errors);
        validarCatalogo(row, "id_siscomex", "G", reglas, errors);
        validarCatalogo(row, "codigo_establecimiento", "H", reglas, errors);
        validarCatalogo(row, "codigo_producto", "N", reglas, errors);
        validarCatalogo(row, "nombre_entidad_muestreo", "T", reglas, errors);
        validarCatalogo(row, "tipo_analisis", "Y", reglas, errors);
        validarCatalogo(row, "analisis_solicitado", "Z", reglas, errors);
        validarCatalogo(row, "unidad_medida", "AB", reglas, errors);

        // D: tipo_formulario — obligatorio siempre en RRA FAR
        String tipoFormulario = isEmpty(row.get("tipo_formulario")) ? "" : row.get("tipo_formulario").toString().trim();
        if (tipoFormulario.isEmpty()) errors.add(err("D", "tipo_formulario", "Campo obligatorio"));
        boolean isFempacSmae = FEMPAC_SMAE.contains(tipoFormulario);

        // F: tipo_control — obligatorio siempre en RRA FAR
        String tipoControl = isEmpty(row.get("tipo_control")) ? "" : row.get("tipo_control").toString().trim();
        if (tipoControl.isEmpty()) errors.add(err("F", "tipo_control", "Campo obligatorio"));

        // G: ID SISCOMEX
        if ("FEMPAC".equals(tipoFormulario) && isEmpty(row.get("id_siscomex"))) {
            errors.add(err("G", "id_siscomex", "Obligatorio cuando el formulario es FEMPAC"));
        }

        // H, I: requeridos para FEMPAC/SMAE
        if (isFempacSmae) {
            if (isEmpty(row.get("codigo_establecimiento"))) errors.add(err("H", "codigo_establecimiento", "Obligatorio para FEMPAC/SMAE"));
            if (isEmpty(row.get("razon_social"))) errors.add(err("I", "razon_social", "Obligatorio para FEMPAC/SMAE"));
        }

        // J, K, L: Centro de cultivo
        if (CENTRO_CULTIVO_CONTROLS.contains(tipoControl)) {
            if (isEmpty(row.get("codigo_centro_cultivo"))) errors.add(err("J", "codigo_centro_cultivo", "Obligatorio para este tipo de control"));
            if (isEmpty(row.get("jaula"))) errors.add(err("L", "jaula", "Obligatorio para este tipo de control"));
        }
        if (!isEmpty(row.get("codigo_centro_cultivo")) && isEmpty(row.get("nombre_centro_cultivo"))) {
            errors.add(err("K", "nombre_centro_cultivo", "Obligatorio cuando se indica código de centro de cultivo"));
        }

        if (isFempacSmae) {
            if (isEmpty(row.get("codigo_producto"))) errors.add(err("N", "codigo_producto", "Obligatorio para FEMPAC/SMAE"));
            if (isEmpty(row.get("linea_proceso"))) errors.add(err("P", "linea_proceso", "Obligatorio para FEMPAC/SMAE"));
        }

        if (isEmpty(row.get("nombre_comun"))) errors.add(err("O", "nombre_comun", "Campo obligatorio"));

        // Q: fecha_elaboracion
        LocalDate fechaElaboracion = null;
        if (isFempacSmae) {
            if (isEmpty(row.get("fecha_elaboracion"))) {
                errors.add(err("Q", "fecha_elaboracion", "Obligatorio para FEMPAC/SMAE"));
            } else {
                fechaElaboracion = validateDateRange(row.get("fecha_elaboracion"), "Q", "fecha_elaboracion", minDate, maxDate, errors);
            }
        }

        // R: Fecha inicio verificación
        LocalDate fechaInicioVer = null;
        if (esVerificacion(tipoControl)) {
            if (isEmpty(row.get("fecha_inicio_verificacion"))) {
                errors.add(err("R", "fecha_inicio_verificacion", "Obligatorio para Verificación Sernapesca/Periódica"));
            } else {
                fechaInicioVer = validateDateRange(row.get("fecha_inicio_verificacion"), "R", "fecha_inicio_verificacion", minDate, maxDate, errors);
                if (fechaInicioVer != null && fechaElaboracion != null && fechaInicioVer.isAfter(fechaElaboracion)) {
                    errors.add(err("R", "fecha_inicio_verificacion", "Debe ser menor o igual a fecha de elaboración"));
                }
            }
        }

        // S: Fecha fin verificación
        LocalDate fechaFinVer = null;
        if (esVerificacion(tipoControl)) {
            if (isEmpty(row.get("fecha_fin_verificacion"))) {
                errors.add(err("S", "fecha_fin_verificacion", "Obligatorio para Verificación Sernapesca/Periódica"));
            } else {
                fechaFinVer = validateDateRange(row.get("fecha_fin_verificacion"), "S", "fecha_fin_verificacion", minDate, maxDatePlus1Y, errors);
                if (fechaFinVer != null && fechaInicioVer != null && fechaFinVer.isBefore(fechaInicioVer)) {
                    errors.add(err("S", "fecha_fin_verificacion", "Debe ser mayor o igual a fecha de inicio de verificación"));
                }
                if (fechaFinVer != null && fechaElaboracion != null && !fechaFinVer.isAfter(fechaElaboracion)) {
                    errors.add(err("S", "fecha_fin_verificacion", "Debe ser mayor a la fecha de elaboración"));
                }
            }
        }

        if (isEmpty(row.get("nombre_entidad_muestreo"))) errors.add(err("T", "nombre_entidad_muestreo", "Campo obligatorio"));

        // U: Fecha muestreo
        LocalDate fechaMuestreo = null;
        if (isEmpty(row.get("fecha_muestreo"))) {
            errors.add(err("U", "fecha_muestreo", "Campo obligatorio"));
        } else {
            fechaMuestreo = validateDateRange(row.get("fecha_muestreo"), "U", "fecha_muestreo", minDate, maxDate, errors);
            if (fechaMuestreo != null && fechaElaboracion != null && !fechaMuestreo.isAfter(fechaElaboracion)) {
                errors.add(err("U", "fecha_muestreo", "Debe ser mayor a la fecha de elaboración"));
            }
            if (fechaMuestreo != null && fechaInicioVer != null && !fechaMuestreo.isAfter(fechaInicioVer)) {
                errors.add(err("U", "fecha_muestreo", "Debe ser mayor que la fecha de inicio de verificación"));
            }
        }

        // V: Fecha envío muestras
        LocalDate fechaEnvio = null;
        if (isEmpty(row.get("fecha_envio_muestras"))) {
            errors.add(err("V", "fecha_envio_muestras", "Campo obligatorio"));
        } else {
            fechaEnvio = validateDateRange(row.get("fecha_envio_muestras"), "V", "fecha_envio_muestras", minDate, maxDate, errors);
            if (fechaEnvio != null && fechaMuestreo != null && fechaEnvio.isBefore(fechaMuestreo)) {
                errors.add(err("V", "fecha_envio_muestras", "Debe ser mayor o igual a la fecha de muestreo"));
            }
        }

        // W: Fecha recepción muestras
        LocalDate fechaRecepcion = null;
        if (isEmpty(row.get("fecha_recepcion_muestras"))) {
            errors.add(err("W", "fecha_recepcion_muestras", "Campo obligatorio"));
        } else {
            fechaRecepcion = validateDateRange(row.get("fecha_recepcion_muestras"), "W", "fecha_recepcion_muestras", minDate, maxDate, errors);
            if (fechaRecepcion != null && fechaEnvio != null && fechaRecepcion.isBefore(fechaEnvio)) {
                errors.add(err("W", "fecha_recepcion_muestras", "Debe ser mayor o igual a fecha de envío"));
            }
        }

        // Campos finales no-fecha obligatorios (los catálogos ya se validaron arriba)
        Map<String, String> obligNoFecha = Map.of(
            "codigo_muestra", "X", "tipo_analisis", "Y", "analisis_solicitado", "Z",
            "valor_obtenido", "AA", "unidad_medida", "AB"
        );
        for (Map.Entry<String, String> entry : obligNoFecha.entrySet()) {
            if (isEmpty(row.get(entry.getKey()))) errors.add(err(entry.getValue(), entry.getKey(), "Campo obligatorio"));
        }

        // Cadena cronológica final: inicio análisis ≥ recepción ≥ ... ≥ emisión (rango 4 años)
        LocalDate fechaInicioAnalisis = validarFechaCadena(row, "fecha_inicio_analisis", "AC", fechaRecepcion, "fecha de recepción de muestras", minDate, maxDate, errors);
        LocalDate fechaObtencion = validarFechaCadena(row, "fecha_obtencion_resultados", "AD", fechaInicioAnalisis, "fecha de inicio de análisis", minDate, maxDate, errors);
        validarFechaCadena(row, "fecha_emision_informe", "AE", fechaObtencion, "fecha de obtención de resultados", minDate, maxDate, errors);

        if (!isEmpty(row.get("externalizacion")) && isEmpty(row.get("nombre_lab_externalizacion"))) {
            errors.add(err("AG", "nombre_lab_externalizacion", "Obligatorio cuando se indica externalización"));
        }

        return new ValidatedRow(rowNumber, errors.isEmpty(), DateUtils.buildDisplay(row), errors);
    }
}