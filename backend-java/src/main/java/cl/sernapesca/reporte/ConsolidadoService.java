package cl.sernapesca.reporte;

import cl.sernapesca.laboratorio.LaboratorioExcelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Genera el consolidado de un período: un único Excel que apila, una debajo de
 * otra, todas las filas de todos los reportes validados de ese período (RRA o
 * RRA FAR). Agrega columnas de procedencia (laboratorio y n° de reporte).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidadoService {

    private final ReporteRepository reporteRepo;
    private final RegistroReporteRepository registroRepo;
    private final ObjectMapper objectMapper;

    /** Encabezados de procedencia que se anteponen a las columnas del reporte. */
    private static final List<String> COLUMNAS_PROCEDENCIA = List.of("laboratorio", "n_reporte");

    public record ConsolidadoArchivo(byte[] contenido, String nombreArchivo, int totalFilas, int totalReportes) {}

    @Transactional(readOnly = true)
    public ConsolidadoArchivo generar(String tipo, int anio, int mes) throws Exception {
        if (!"RRA".equals(tipo) && !"RRA_FAR".equals(tipo)) {
            throw new IllegalArgumentException("Tipo inválido: " + tipo + ". Use RRA o RRA_FAR");
        }

        List<Reporte> reportes = reporteRepo.findByAnioAndMesAndTipoOrderByFechaEnvioDesc(anio, mes, tipo);
        if (reportes.isEmpty()) {
            throw new NoSuchElementException(String.format(
                "No hay reportes %s validados para el período %02d/%d", tipo, mes, anio));
        }

        List<String> columnas = "RRA".equals(tipo)
                ? LaboratorioExcelService.RRA_COLUMNS
                : LaboratorioExcelService.RRA_FAR_COLUMNS;

        int totalFilas = 0;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("consolidado");

            // Fila de encabezado: procedencia + columnas del reporte
            Row header = sheet.createRow(0);
            int c = 0;
            CellStyle headerStyle = estiloHeader(wb);
            for (String col : COLUMNAS_PROCEDENCIA) {
                Cell cell = header.createCell(c++);
                cell.setCellValue(col);
                cell.setCellStyle(headerStyle);
            }
            for (String col : columnas) {
                Cell cell = header.createCell(c++);
                cell.setCellValue(col);
                cell.setCellStyle(headerStyle);
            }

            // Apila los registros de cada reporte
            int rowIdx = 1;
            for (Reporte reporte : reportes) {
                List<RegistroReporte> registros = registroRepo.findByReporteIdOrderByNumeroFila(reporte.getId());
                for (RegistroReporte reg : registros) {
                    Map<String, Object> datos = objectMapper.readValue(
                            reg.getDatosJson(), new TypeReference<Map<String, Object>>() {});
                    Row row = sheet.createRow(rowIdx++);
                    int cc = 0;
                    row.createCell(cc++).setCellValue(safe(reporte.getNombreLaboratorio()));
                    row.createCell(cc++).setCellValue(reporte.getId());
                    for (String col : columnas) {
                        Object val = datos.get(col);
                        row.createCell(cc++).setCellValue(val == null ? "" : val.toString());
                    }
                    totalFilas++;
                }
            }

            wb.write(out);
            String nombre = String.format("consolidado_%s_%d_%02d.xlsx", tipo.toLowerCase(), anio, mes);
            log.info("Consolidado {} {}/{}: {} filas de {} reportes", tipo, mes, anio, totalFilas, reportes.size());
            return new ConsolidadoArchivo(out.toByteArray(), nombre, totalFilas, reportes.size());
        }
    }

    private CellStyle estiloHeader(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
