package cl.sernapesca.laboratorio.validators;

import java.util.List;

public class ValidationTypes {

    public record FieldError(
        String column,
        String field,
        String message
    ) {}

    public record RowDisplay(
        String codigoMuestra,
        String nInforme,
        String analisis,
        String valorObtenido,
        String fechaMuestreo
    ) {}

    public record ValidatedRow(
        int rowNumber,
        boolean isValid,
        RowDisplay display,
        List<FieldError> errors
    ) {}

    public record ValidationResult(
        String templateType,
        int totalRows,
        int validRows,
        int errorRows,
        List<ValidatedRow> rows
    ) {}
}