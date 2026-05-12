import { Injectable, BadRequestException } from '@nestjs/common';
import * as XLSX from 'xlsx';
import { TemplateType, ValidationResult } from './types/validation.types';
import { RRA_COLUMNS, validateRraRow } from './validators/rra.validator';
import { RRA_FAR_COLUMNS, validateRraFarRow } from './validators/rra-far.validator';

@Injectable()
export class LaboratoriosService {
  validateFile(buffer: Buffer, _mimetype: string, templateType: TemplateType): ValidationResult {
    console.log(`[Lab] Recibido: ${buffer.length} bytes, plantilla: ${templateType}`);

    let workbook: XLSX.WorkBook;
    try {
      workbook = XLSX.read(buffer, { type: 'buffer', cellDates: true });
    } catch (e) {
      console.error('[Lab] Error parseando archivo:', e);
      throw new BadRequestException('No se pudo leer el archivo. Verifique que sea .xlsx, .xlsm o .csv válido.');
    }

    console.log('[Lab] Hojas:', workbook.SheetNames);

    // SheetNames[0] = Versión, SheetNames[1] = ingreso
    const sheetName = workbook.SheetNames[1] ?? workbook.SheetNames[0];
    if (!sheetName) throw new BadRequestException('El archivo no contiene hojas de datos.');

    console.log(`[Lab] Hoja: "${sheetName}"`);

    const sheet = workbook.Sheets[sheetName];
    const columns = templateType === 'RRA' ? RRA_COLUMNS : RRA_FAR_COLUMNS;

    const rawRows: unknown[][] = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: null });

    console.log(`[Lab] Filas leídas: ${rawRows.length}`);

    if (rawRows.length < 2) {
      return { templateType, totalRows: 0, validRows: 0, errorRows: 0, rows: [] };
    }

    const DATA_START_ROW = 1;
    const dataRows = rawRows
      .slice(DATA_START_ROW)
      .filter((row) => this.hasAnyData(row as unknown[]));

    console.log(`[Lab] Filas con datos: ${dataRows.length}`);

    const isRra = templateType === 'RRA';

    const results = dataRows.map((rawRow, idx) => {
      const arr = rawRow as unknown[];
      const rowData: Record<string, unknown> = {};
      columns.forEach(({ field }, colIdx) => {
        rowData[field] = arr[colIdx] ?? null;
      });

      const rowFn = isRra ? validateRraRow : validateRraFarRow;
      const validated = rowFn(rowData, DATA_START_ROW + idx + 1);

      const fmtDate = (v: unknown): string => {
        if (v == null) return '—';
        if (v instanceof Date && !isNaN(v.getTime())) {
          const d = v.getDate().toString().padStart(2, '0');
          const m = (v.getMonth() + 1).toString().padStart(2, '0');
          const y = v.getFullYear();
          return `${d}/${m}/${y}`;
        }
        return String(v).trim() || '—';
      };
      const str = (v: unknown) => (v != null ? String(v).trim() : '—');

      return {
        rowNumber: validated.rowNumber,
        isValid: validated.isValid,
        display: {
          codigoMuestra: str(rowData['codigo_muestra']),
          nInforme:      str(rowData['n_informe']),
          analisis:      str(rowData['analisis_solicitado'] ?? rowData['tipo_analisis']),
          valorObtenido: str(rowData['valor_obtenido']),
          fechaMuestreo: fmtDate(rowData['fecha_muestreo'] ?? rowData['fecha_elaboracion']),
        },
        errors: validated.errors,
      };
    });

    const validRows = results.filter((r) => r.isValid).length;
    const errorRows = results.filter((r) => !r.isValid);
    console.log(`[Lab] OK: ${validRows}, errores: ${errorRows.length}`);
    // Log first 5 error rows for debugging
    errorRows.slice(0, 5).forEach((r) => {
      console.log(`[Lab] Fila ${r.rowNumber} errores:`, JSON.stringify(r.errors));
    });

    return {
      templateType,
      totalRows: results.length,
      validRows,
      errorRows: results.length - validRows,
      rows: results,
    };
  }

  private hasAnyData(row: unknown[]): boolean {
    return row.some((c) => c !== null && c !== undefined && String(c).trim() !== '');
  }
}
