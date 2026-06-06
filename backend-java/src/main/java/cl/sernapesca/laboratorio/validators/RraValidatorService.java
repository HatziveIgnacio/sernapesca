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
    private static final Set<String> VERIFICACION_TYPES = Set.of("Verificación Sernapesca", "Verificación periódica PAC");
    private static final String FEMPAC = "FEMPAC";

    private boolean isEmpty(Object v) {
        return v == null || v.toString().trim().isEmpty();
    }

    private FieldError err(String col, String field, String msg) {
        return new FieldError(col, field, msg);
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

    public ValidatedRow validate(Map<String, Object> row, int rowNumber) {
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

        // Tipo Formulario
        String tipoFormulario = isEmpty(row.get("tipo_formulario")) ? "" : row.get("tipo_formulario").toString().trim();
        if (tipoFormulario.isEmpty()) {
            errors.add(err("D", "tipo_formulario", "Campo obligatorio"));
        } else if (!List.of("FEMPAC", "SMAE").contains(tipoFormulario)) {
            errors.add(err("D", "tipo_formulario", "Debe ser FEMPAC o SMAE"));
        }

        // Tipo Control
        String tipoControl = isEmpty(row.get("tipo_control")) ? "" : row.get("tipo_control").toString().trim();
        List<String> validControls = List.of("Verificación Sernapesca", "Verificación periódica PAC", "Acción correctiva debido a un desfavorable anterior", "Control de producto final");
        if (tipoControl.isEmpty()) {
            errors.add(err("F", "tipo_control", "Campo obligatorio"));
        } else if (!validControls.contains(tipoControl)) {
            errors.add(err("F", "tipo_control", "Valor no válido para tipo de control"));
        }

        if (FEMPAC.equals(tipoFormulario) && isEmpty(row.get("id_siscomex"))) {
            errors.add(err("G", "id_siscomex", "Obligatorio cuando el formulario es FEMPAC"));
        }

        if (isEmpty(row.get("codigo_establecimiento"))) errors.add(err("H", "codigo_establecimiento", "Campo obligatorio"));
        if (isEmpty(row.get("razon_social"))) errors.add(err("I", "razon_social", "Campo obligatorio"));

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
        if (VERIFICACION_TYPES.contains(tipoControl)) {
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
        if (VERIFICACION_TYPES.contains(tipoControl)) {
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

        // Bloque final de campos requeridos
        Map<String, String> endFields = Map.of(
            "fecha_envio_muestras", "U", "fecha_recepcion_muestras", "V",
            "codigo_muestra", "W", "tipo_analisis", "X", "analisis_solicitado", "Y",
            "valor_obtenido", "Z", "unidad_medida", "AA", "fecha_inicio_analisis", "AB",
            "fecha_obtencion_resultados", "AC", "fecha_emision_informe", "AD"
        );
        for (Map.Entry<String, String> entry : endFields.entrySet()) {
            if (isEmpty(row.get(entry.getKey()))) errors.add(err(entry.getValue(), entry.getKey(), "Campo obligatorio"));
        }

        return new ValidatedRow(rowNumber, errors.isEmpty(), DateUtils.buildDisplay(row), errors);
    }
}