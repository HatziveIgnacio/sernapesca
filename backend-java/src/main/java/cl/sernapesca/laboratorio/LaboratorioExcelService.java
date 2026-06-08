package cl.sernapesca.laboratorio;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import cl.sernapesca.laboratorio.validators.RraValidatorService;
import cl.sernapesca.laboratorio.validators.RraFarValidatorService;
import cl.sernapesca.laboratorio.validators.ReglasDinamicas;
import cl.sernapesca.laboratorio.validators.ValidationTypes.ValidatedRow;
import cl.sernapesca.laboratorio.validators.ValidationTypes.ValidationResult;
import cl.sernapesca.periodo.PeriodoReporte;
import cl.sernapesca.periodo.PeriodoReporteRepository;
import cl.sernapesca.periodo.ValorPermitidoRepository;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class LaboratorioExcelService {

    private final RraValidatorService rraValidator;
    private final RraFarValidatorService rraFarValidator;
    private final PeriodoReporteRepository periodoRepo;
    private final ValorPermitidoRepository valorRepo;

    public LaboratorioExcelService(RraValidatorService rraValidator,
                                   RraFarValidatorService rraFarValidator,
                                   PeriodoReporteRepository periodoRepo,
                                   ValorPermitidoRepository valorRepo) {
        this.rraValidator = rraValidator;
        this.rraFarValidator = rraFarValidator;
        this.periodoRepo = periodoRepo;
        this.valorRepo = valorRepo;
    }

    /**
     * Carga las reglas dinámicas (listas de valores permitidos) del período
     * correspondiente. Si se indican anio/mes, busca ese período exacto; si no,
     * usa el período ABIERTO más reciente del tipo. Si no hay período o no tiene
     * reglas extraídas, devuelve reglas vacías (solo se valida estructura).
     */
    private ReglasDinamicas cargarReglas(String templateType, Integer anio, Integer mes) {
        Optional<PeriodoReporte> periodo = (anio != null && mes != null)
            ? periodoRepo.findByAnioAndMesAndTipo(anio, mes, templateType)
            : periodoRepo.findFirstByTipoAndEstadoOrderByAnioDescMesDesc(
                  templateType, cl.sernapesca.periodo.EstadoPeriodo.ABIERTO);

        if (periodo.isEmpty()) {
            log.warn("Validación sin período para tipo={} anio={} mes={}: solo validación estructural",
                    templateType, anio, mes);
            return ReglasDinamicas.vacio();
        }

        var valores = valorRepo.findByPeriodoId(periodo.get().getId());
        if (valores.isEmpty()) {
            log.warn("Período {} sin reglas extraídas: solo validación estructural", periodo.get().getId());
            return ReglasDinamicas.vacio();
        }
        log.info("Validando contra {} valores permitidos del período {}", valores.size(), periodo.get().getId());
        return ReglasDinamicas.desde(valores);
    }

    public static final List<String> RRA_COLUMNS = Arrays.asList(
        "nombre_laboratorio", "tipo_laboratorio", "n_informe", "tipo_formulario", "n_formulario",
        "tipo_control", "id_siscomex", "codigo_establecimiento", "razon_social", "codigo_area_extraccion",
        "fecha_extraccion", "tipo_consumo", "codigo_producto", "nombre_comun", "linea_proceso",
        "fecha_elaboracion", "fecha_inicio_verificacion", "fecha_fin_verificacion", "nombre_entidad_muestreo",
        "fecha_muestreo", "fecha_envio_muestras", "fecha_recepcion_muestras", "codigo_muestra", "tipo_analisis",
        "analisis_solicitado", "valor_obtenido", "unidad_medida", "fecha_inicio_analisis", "fecha_obtencion_resultados",
        "fecha_emision_informe", "externalizacion", "nombre_lab_externalizacion", "anula_reemplaza"
    );

    public static final List<String> RRA_FAR_COLUMNS = Arrays.asList(
        "nombre_laboratorio", "tipo_laboratorio", "n_informe", "tipo_formulario", "n_formulario",
        "tipo_control", "id_siscomex", "codigo_establecimiento", "razon_social", "codigo_centro_cultivo",
        "nombre_centro_cultivo", "jaula", "tipo_consumo", "codigo_producto", "nombre_comun",
        "linea_proceso", "fecha_elaboracion", "fecha_inicio_verificacion", "fecha_fin_verificacion",
        "nombre_entidad_muestreo", "fecha_muestreo", "fecha_envio_muestras", "fecha_recepcion_muestras",
        "codigo_muestra", "tipo_analisis", "analisis_solicitado", "valor_obtenido", "unidad_medida",
        "fecha_inicio_analisis", "fecha_obtencion_resultados", "fecha_emision_informe", "externalizacion",
        "nombre_lab_externalizacion", "anula_reemplaza"
    );

    /** Una fila parseada: sus datos crudos por columna + el resultado de validación. */
    public record FilaConDatos(int numeroFila, Map<String, Object> datos, ValidatedRow validacion) {}

    /**
     * Parsea y valida el archivo, devolviendo cada fila con sus datos crudos
     * completos (no solo el display). Lo usan tanto validateFile (para la UI)
     * como la finalización (para persistir los registros).
     */
    public List<FilaConDatos> parsearYValidar(MultipartFile file, String templateType,
                                              Integer anio, Integer mes) throws Exception {
        List<FilaConDatos> filas = new ArrayList<>();
        boolean isRra = "RRA".equals(templateType);
        List<String> columns = isRra ? RRA_COLUMNS : RRA_FAR_COLUMNS;

        ReglasDinamicas reglas = cargarReglas(templateType, anio, mes);

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            // SheetNames[1] = ingreso, SheetNames[0] = Versión
            Sheet sheet = workbook.getNumberOfSheets() > 1 ? workbook.getSheetAt(1) : workbook.getSheetAt(0);

            // Detecta el tipo real del archivo y verifica que coincida con el seleccionado
            String tipoReal = detectarTipo(sheet);
            if (tipoReal != null && !tipoReal.equals(templateType)) {
                String legible = tipoReal.equals("RRA_FAR") ? "RRA FAR" : "RRA";
                String seleccionado = "RRA_FAR".equals(templateType) ? "RRA FAR" : "RRA";
                throw new IllegalArgumentException(
                    "El archivo corresponde a una plantilla " + legible + ", pero seleccionó " + seleccionado +
                    ". Seleccione el tipo de plantilla correcto.");
            }

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
                    rraValidator.validate(rowData, rowNumber, reglas) :
                    rraFarValidator.validate(rowData, rowNumber, reglas);

                filas.add(new FilaConDatos(rowNumber, rowData, validated));
            }
        }
        return filas;
    }

    public ValidationResult validateFile(MultipartFile file, String templateType,
                                         Integer anio, Integer mes) throws Exception {
        List<ValidatedRow> results = parsearYValidar(file, templateType, anio, mes)
                .stream().map(FilaConDatos::validacion).toList();

        int validRows = (int) results.stream().filter(ValidatedRow::isValid).count();
        return new ValidationResult(
            templateType,
            results.size(),
            validRows,
            results.size() - validRows,
            results
        );
    }

    /**
     * Detecta el tipo de plantilla por el encabezado de la columna J: en RRA es
     * "Código área extracción", en RRA FAR es "Código Centro de Cultivo".
     * Devuelve "RRA", "RRA_FAR" o null si no puede determinarlo.
     */
    private String detectarTipo(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) return null;
        Cell celdaJ = header.getCell(9); // columna J (0-based índice 9)
        Object val = getCellValue(celdaJ);
        if (val == null) return null;
        String j = java.text.Normalizer.normalize(val.toString(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase().trim();
        if (j.contains("centro de cultivo")) return "RRA_FAR";
        if (j.contains("area extraccion") || j.contains("area de extraccion")) return "RRA";
        return null;
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
            case NUMERIC -> org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue() : numeric(cell.getNumericCellValue());
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getRichStringCellValue().getString();
                case NUMERIC -> org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)
                        ? cell.getDateCellValue() : numeric(cell.getNumericCellValue());
                case BOOLEAN -> cell.getBooleanCellValue();
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * Enteros como Long (toString "123", no "123.0") para que coincidan con la
     * normalización del extractor de catálogos. Decimales reales se mantienen.
     */
    private Object numeric(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return (long) d;
        return d;
    }
}