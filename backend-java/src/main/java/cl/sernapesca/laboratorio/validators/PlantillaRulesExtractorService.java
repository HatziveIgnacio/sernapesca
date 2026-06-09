package cl.sernapesca.laboratorio.validators;

import cl.sernapesca.periodo.ValorPermitido;
import cl.sernapesca.periodo.ValorPermitidoRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Extrae las listas maestras de valores permitidos desde la plantilla Excel
 * (.xlsm) que SERNAPESCA sube cada mes, y las persiste en `valor_permitido`
 * asociadas a un período.
 *
 * El VBA original valida cada columna del reporte haciendo VLookups contra
 * tablas (ListObjects) en hojas ocultas. Aquí localizamos esas tablas POR
 * NOMBRE y guardamos sus valores. Cuando un laboratorio sube su reporte, el
 * validador compara contra estos valores en lugar de listas hardcodeadas.
 */
@Slf4j
@Service
public class PlantillaRulesExtractorService {

    private final ValorPermitidoRepository repo;

    public PlantillaRulesExtractorService(ValorPermitidoRepository repo) {
        this.repo = repo;
    }

    /**
     * Mapeo: HEADER de la tabla (texto de la primera celda) → campo del reporte.
     *
     * Se mapea por header, NO por el nombre del ListObject: ese nombre es
     * inconsistente entre plantillas (en unas es "Form_SERNAPESCA", en otras
     * "Table_6"), mientras el header siempre es semántico y consistente.
     * Las claves están normalizadas (minúscula, sin tildes) — ver normalizarHeader().
     * Las tablas de análisis se unifican bajo "analisis_solicitado".
     */
    private static final Map<String, String> HEADER_A_CAMPO = Map.ofEntries(
        Map.entry("form_sernapesca",        "tipo_formulario"),
        Map.entry("tipo_control",           "tipo_control"),
        Map.entry("tipo_consumo",           "tipo_consumo"),
        Map.entry("tipo_lab",               "tipo_laboratorio"),
        Map.entry("tp_analisis",            "tipo_analisis"),
        Map.entry("u_medida",               "unidad_medida"),
        Map.entry("nombre_ent",             "nombre_entidad_muestreo"),
        Map.entry("nombre_lab",             "nombre_laboratorio"),
        Map.entry("id",                     "id_siscomex"),
        Map.entry("codigo establecimiento", "codigo_establecimiento"),
        Map.entry("codareaextraccion",      "codigo_area_extraccion"),
        Map.entry("codigo producto",        "codigo_producto"),
        // Tablas de análisis → todas al mismo campo "analisis_solicitado"
        Map.entry("microbiologicos",        "analisis_solicitado"),
        Map.entry("quimicos",               "analisis_solicitado"),
        Map.entry("bioensayo",              "analisis_solicitado"),
        Map.entry("organoleptico",          "analisis_solicitado"),
        Map.entry("radiologico",            "analisis_solicitado")
    );

    /** Normaliza un header a minúscula sin tildes para el match del mapeo. */
    private static String normalizarHeader(String h) {
        if (h == null) return "";
        String sinTilde = java.text.Normalizer.normalize(h, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTilde.trim().toLowerCase();
    }

    public record ExtraccionResumen(long periodoId, int totalValores, Map<String, Integer> porCampo) {}

    /**
     * Extrae y persiste las listas maestras de la plantilla para un período.
     * Es idempotente: borra los valores previos del período antes de insertar.
     */
    public ExtraccionResumen extraerYGuardar(Path plantillaPath, long periodoId) throws Exception {
        if (!Files.exists(plantillaPath)) {
            throw new IllegalArgumentException("No existe el archivo de plantilla: " + plantillaPath);
        }

        // campo → conjunto de valores (Set evita duplicados al unificar tablas de análisis)
        Map<String, Set<String>> valoresPorCampo = new HashMap<>();

        try (InputStream is = Files.newInputStream(plantillaPath);
             XSSFWorkbook wb = new XSSFWorkbook(is)) {

            // Recorre todas las hojas y todas sus tablas (ListObjects)
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                XSSFSheet sheet = wb.getSheetAt(s);
                for (XSSFTable table : sheet.getTables()) {
                    String header = leerHeader(sheet, table);
                    String campo = HEADER_A_CAMPO.get(normalizarHeader(header));
                    if (campo == null) continue; // tabla no mapeada, se ignora

                    List<String> valores = leerPrimeraColumna(sheet, table);
                    valoresPorCampo.computeIfAbsent(campo, k -> new LinkedHashSet<>()).addAll(valores);
                    log.debug("Tabla header='{}' ({}) → campo '{}': {} valores",
                            header, table.getName(), campo, valores.size());
                }
            }
        }

        if (valoresPorCampo.isEmpty()) {
            log.warn("Plantilla {} no contiene tablas maestras reconocidas", plantillaPath.getFileName());
        }

        // Persistencia idempotente
        repo.deleteByPeriodoId(periodoId);

        List<ValorPermitido> aGuardar = new ArrayList<>();
        Map<String, Integer> porCampo = new TreeMap<>();
        for (var entry : valoresPorCampo.entrySet()) {
            String campo = entry.getKey();
            for (String valor : entry.getValue()) {
                aGuardar.add(ValorPermitido.builder()
                        .periodoId(periodoId)
                        .campo(campo)
                        .valor(valor)
                        .build());
            }
            porCampo.put(campo, entry.getValue().size());
        }
        repo.saveAll(aGuardar);

        log.info("Extracción período {}: {} valores en {} campos", periodoId, aGuardar.size(), porCampo.size());
        return new ExtraccionResumen(periodoId, aGuardar.size(), porCampo);
    }

    /** Lee el texto del header (primera celda del rango de la tabla). */
    private String leerHeader(XSSFSheet sheet, XSSFTable table) {
        AreaReference area = new AreaReference(table.getCTTable().getRef(),
                sheet.getWorkbook().getSpreadsheetVersion());
        CellReference first = area.getFirstCell();
        Row row = sheet.getRow(first.getRow());
        if (row == null) return "";
        return cellToString(row.getCell(first.getCol()));
    }

    /**
     * Lee la primera columna del rango de datos de una tabla, saltando la fila
     * de encabezado. Trunca a 500 chars (límite de la columna en BD).
     */
    private List<String> leerPrimeraColumna(XSSFSheet sheet, XSSFTable table) {
        List<String> out = new ArrayList<>();
        AreaReference area = new AreaReference(table.getCTTable().getRef(),
                sheet.getWorkbook().getSpreadsheetVersion());
        CellReference first = area.getFirstCell();
        CellReference last = area.getLastCell();

        int col = first.getCol();
        int startRow = first.getRow() + table.getHeaderRowCount(); // salta encabezado(s)
        int endRow = last.getRow();

        for (int r = startRow; r <= endRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Cell cell = row.getCell(col);
            String val = cellToString(cell);
            if (val != null && !val.isBlank()) {
                if (val.length() > 500) val = val.substring(0, 500);
                out.add(val.trim());
            }
        }
        return out;
    }

    private String cellToString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                // códigos numéricos sin decimales innecesarios (ej. 12345 no "12345.0")
                if (d == Math.floor(d) && !Double.isInfinite(d)) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> null;
        };
    }
}
