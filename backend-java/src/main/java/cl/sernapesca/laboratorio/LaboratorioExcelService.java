package cl.sernapesca.laboratorio;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import cl.sernapesca.laboratorio.validators.RraValidatorService;
import cl.sernapesca.laboratorio.validators.RraFarValidatorService;
import cl.sernapesca.laboratorio.validators.ValidationTypes.ValidatedRow;
import cl.sernapesca.laboratorio.validators.ValidationTypes.ValidationResult;

import java.io.InputStream;
import java.util.*;

@Service
public class LaboratorioExcelService {

    private final RraValidatorService rraValidator;
    private final RraFarValidatorService rraFarValidator;

    public LaboratorioExcelService(RraValidatorService rraValidator, RraFarValidatorService rraFarValidator) {
        this.rraValidator = rraValidator;
        this.rraFarValidator = rraFarValidator;
    }

    private static final List<String> RRA_COLUMNS = Arrays.asList(
        "nombre_laboratorio", "tipo_laboratorio", "n_informe", "tipo_formulario", "n_formulario",
        "tipo_control", "id_siscomex", "codigo_establecimiento", "razon_social", "codigo_area_extraccion",
        "fecha_extraccion", "tipo_consumo", "codigo_producto", "nombre_comun", "linea_proceso",
        "fecha_elaboracion", "fecha_inicio_verificacion", "fecha_fin_verificacion", "nombre_entidad_muestreo",
        "fecha_muestreo", "fecha_envio_muestras", "fecha_recepcion_muestras", "codigo_muestra", "tipo_analisis",
        "analisis_solicitado", "valor_obtenido", "unidad_medida", "fecha_inicio_analisis", "fecha_obtencion_resultados",
        "fecha_emision_informe", "externalizacion", "nombre_lab_externalizacion", "anula_reemplaza"
    );

    private static final List<String> RRA_FAR_COLUMNS = Arrays.asList(
        "nombre_laboratorio", "tipo_laboratorio", "n_informe", "tipo_formulario", "n_formulario",
        "tipo_control", "id_siscomex", "codigo_establecimiento", "razon_social", "codigo_centro_cultivo",
        "nombre_centro_cultivo", "jaula", "tipo_consumo", "codigo_producto", "nombre_comun",
        "linea_proceso", "fecha_elaboracion", "fecha_inicio_verificacion", "fecha_fin_verificacion",
        "nombre_entidad_muestreo", "fecha_muestreo", "fecha_envio_muestras", "fecha_recepcion_muestras",
        "codigo_muestra", "tipo_analisis", "analisis_solicitado", "valor_obtenido", "unidad_medida",
        "fecha_inicio_analisis", "fecha_obtencion_resultados", "fecha_emision_informe", "externalizacion",
        "nombre_lab_externalizacion", "anula_reemplaza"
    );

    public ValidationResult validateFile(MultipartFile file, String templateType) throws Exception {
        List<ValidatedRow> results = new ArrayList<>();
        boolean isRra = "RRA".equals(templateType);
        List<String> columns = isRra ? RRA_COLUMNS : RRA_FAR_COLUMNS;

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            // SheetNames[1] = ingreso, SheetNames[0] = Versión
            Sheet sheet = workbook.getNumberOfSheets() > 1 ? workbook.getSheetAt(1) : workbook.getSheetAt(0);

            int dataStartRow = 1; // Saltamos la cabecera
            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || !hasAnyData(row)) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    rowData.put(columns.get(colIdx), getCellValue(cell));
                }

                int rowNumber = i + 1; // 1-based para la UI
                ValidatedRow validated = isRra ? 
                    rraValidator.validate(rowData, rowNumber) : 
                    rraFarValidator.validate(rowData, rowNumber);
                
                results.add(validated);
            }
        }

        int validRows = (int) results.stream().filter(ValidatedRow::isValid).count();
        return new ValidationResult(
            templateType,
            results.size(),
            validRows,
            results.size() - validRows,
            results
        );
    }

    private boolean hasAnyData(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = cell.toString().trim();
                if (!val.isEmpty()) return true;
            }
        }
        return false;
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getRichStringCellValue().getString();
                case NUMERIC -> org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
                case BOOLEAN -> cell.getBooleanCellValue();
                default -> null;
            };
            default -> null;
        };
    }
}