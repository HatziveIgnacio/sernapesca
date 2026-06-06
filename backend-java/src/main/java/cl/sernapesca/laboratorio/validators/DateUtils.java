package cl.sernapesca.laboratorio.validators;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

    public static LocalDate parseDate(Object value) {
        if (value == null) return null;

        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof Number) {
            // Manejo de fechas seriales de Excel (días desde el 01/01/1900)
            long excelSerial = ((Number) value).longValue();
            long utcDays = excelSerial - 25569;
            return LocalDate.of(1970, 1, 1).plusDays(utcDays);
        }
        if (value instanceof String) {
            String trimmed = ((String) value).trim();
            if (trimmed.isEmpty()) return null;

            // Intentar formato dd/mm/aaaa
            try {
                return LocalDate.parse(trimmed, FORMATTER);
            } catch (DateTimeParseException e) {
                // Fallback a parseo ISO estándar (aaaa-mm-dd)
                try {
                    return LocalDate.parse(trimmed);
                } catch (DateTimeParseException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate addYears(LocalDate date, int years) {
        return date.plusYears(years);
    }
    public static ValidationTypes.RowDisplay buildDisplay(Map<String, Object> rowData) {
        String codigoMuestra = str(rowData.get("codigo_muestra"));
        String nInforme = str(rowData.get("n_informe"));
        Object fMuestreo = rowData.get("fecha_muestreo") != null ? rowData.get("fecha_muestreo") : rowData.get("fecha_elaboracion");
        String fechaMuestreo = fmtDate(fMuestreo);
        
        Object analisisObj = rowData.get("analisis_solicitado");
        if (analisisObj == null || String.valueOf(analisisObj).trim().isEmpty()) {
            analisisObj = rowData.get("tipo_analisis");
        }
        String analisis = str(analisisObj);
        String valorObtenido = str(rowData.get("valor_obtenido"));

        return new ValidationTypes.RowDisplay(codigoMuestra, nInforme, analisis, valorObtenido, fechaMuestreo);
    }

    private static String str(Object v) {
        if (v == null) return "—";
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? "—" : s;
    }

    private static String fmtDate(Object v) {
        if (v == null) return "—";
        if (v instanceof Date d) {
            return new java.text.SimpleDateFormat("dd/MM/yyyy").format(d);
        }
        if (v instanceof LocalDate ld) {
            return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? "—" : s;
    }
}