import { FieldError, ValidatedRow } from '../types/validation.types';
import { parseDate, today, addYears, dateOnly } from './date.utils';

// Column letter → field name mapping for RRA (0-indexed)
export const RRA_COLUMNS: { col: string; field: string }[] = [
  { col: 'A', field: 'nombre_laboratorio' },
  { col: 'B', field: 'tipo_laboratorio' },
  { col: 'C', field: 'n_informe' },
  { col: 'D', field: 'tipo_formulario' },
  { col: 'E', field: 'n_formulario' },
  { col: 'F', field: 'tipo_control' },
  { col: 'G', field: 'id_siscomex' },
  { col: 'H', field: 'codigo_establecimiento' },
  { col: 'I', field: 'razon_social' },
  { col: 'J', field: 'codigo_area_extraccion' },
  { col: 'K', field: 'fecha_extraccion' },
  { col: 'L', field: 'tipo_consumo' },
  { col: 'M', field: 'codigo_producto' },
  { col: 'N', field: 'nombre_comun' },
  { col: 'O', field: 'linea_proceso' },
  { col: 'P', field: 'fecha_elaboracion' },
  { col: 'Q', field: 'fecha_inicio_verificacion' },
  { col: 'R', field: 'fecha_fin_verificacion' },
  { col: 'S', field: 'nombre_entidad_muestreo' },
  { col: 'T', field: 'fecha_muestreo' },
  { col: 'U', field: 'fecha_envio_muestras' },
  { col: 'V', field: 'fecha_recepcion_muestras' },
  { col: 'W', field: 'codigo_muestra' },
  { col: 'X', field: 'tipo_analisis' },
  { col: 'Y', field: 'analisis_solicitado' },
  { col: 'Z', field: 'valor_obtenido' },
  { col: 'AA', field: 'unidad_medida' },
  { col: 'AB', field: 'fecha_inicio_analisis' },
  { col: 'AC', field: 'fecha_obtencion_resultados' },
  { col: 'AD', field: 'fecha_emision_informe' },
  { col: 'AE', field: 'externalizacion' },
  { col: 'AF', field: 'nombre_lab_externalizacion' },
  { col: 'AG', field: 'anula_reemplaza' },
];

const VERIFICACION_TYPES = new Set([
  'Verificación Sernapesca',
  'Verificación periódica PAC',
]);

const FEMPAC = 'FEMPAC';

function isEmpty(v: unknown): boolean {
  return v === null || v === undefined || String(v).trim() === '';
}

function err(col: string, field: string, msg: string): FieldError {
  return { column: col, field, message: msg };
}

function validateDateRange(
  value: unknown,
  col: string,
  field: string,
  minDate: Date,
  maxDate: Date,
  errors: FieldError[],
): Date | null {
  const d = parseDate(value);
  if (!d) {
    errors.push(err(col, field, 'Fecha inválida. Use formato dd/mm/aaaa'));
    return null;
  }
  const dc = dateOnly(d);
  if (dc < minDate) {
    errors.push(err(col, field, `Fecha anterior al mínimo permitido (${minDate.toLocaleDateString('es-CL')})`));
    return dc;
  }
  if (dc > maxDate) {
    errors.push(err(col, field, `Fecha posterior al máximo permitido (${maxDate.toLocaleDateString('es-CL')})`));
    return dc;
  }
  return dc;
}

export function validateRraRow(
  row: Record<string, unknown>,
  rowNumber: number,
): ValidatedRow {
  const errors: FieldError[] = [];
  const now = today();
  const minDate = addYears(now, -4);
  const maxDate = now;
  const maxDatePlus1Y = addYears(now, 1);

  const get = (field: string) => row[field];
  const getStr = (field: string): string => String(get(field) ?? '').trim();

  // ── Simple required fields ──────────────────────────────────────────────
  for (const f of ['nombre_laboratorio', 'tipo_laboratorio', 'n_informe', 'n_formulario']) {
    const col = RRA_COLUMNS.find((c) => c.field === f)!.col;
    if (isEmpty(get(f))) errors.push(err(col, f, 'Campo obligatorio'));
  }

  // D: tipo_formulario
  const tipoFormulario = getStr('tipo_formulario');
  if (isEmpty(tipoFormulario)) {
    errors.push(err('D', 'tipo_formulario', 'Campo obligatorio'));
  } else if (!['FEMPAC', 'SMAE'].includes(tipoFormulario)) {
    errors.push(err('D', 'tipo_formulario', 'Debe ser FEMPAC o SMAE'));
  }

  // F: tipo_control
  const tipoControl = getStr('tipo_control');
  const validControls = [
    'Verificación Sernapesca',
    'Verificación periódica PAC',
    'Acción correctiva debido a un desfavorable anterior',
    'Control de producto final',
  ];
  if (isEmpty(tipoControl)) {
    errors.push(err('F', 'tipo_control', 'Campo obligatorio'));
  } else if (!validControls.includes(tipoControl)) {
    errors.push(err('F', 'tipo_control', 'Valor no válido para tipo de control'));
  }

  // G: ID SISCOMEX — required when tipo_formulario is FEMPAC
  if (tipoFormulario === FEMPAC && isEmpty(get('id_siscomex'))) {
    errors.push(err('G', 'id_siscomex', 'Obligatorio cuando el formulario es FEMPAC'));
  }

  // H: Código Establecimiento — required
  if (isEmpty(get('codigo_establecimiento'))) {
    errors.push(err('H', 'codigo_establecimiento', 'Campo obligatorio'));
  }

  // I: Razón Social — required
  if (isEmpty(get('razon_social'))) {
    errors.push(err('I', 'razon_social', 'Campo obligatorio'));
  }

  // J: Código área extracción — optional, but if FEMPAC with código then K required
  const codigoArea = getStr('codigo_area_extraccion');

  // K: Fecha extracción — required if J filled
  let fechaExtraccion: Date | null = null;
  if (!isEmpty(codigoArea)) {
    if (isEmpty(get('fecha_extraccion'))) {
      errors.push(err('K', 'fecha_extraccion', 'Obligatorio cuando se indica código de área'));
    } else {
      fechaExtraccion = validateDateRange(get('fecha_extraccion'), 'K', 'fecha_extraccion', minDate, maxDate, errors);
    }
  }

  // L: Tipo de consumo — required
  const validConsumo = ['Consumo Humano', 'No Consumo Humano', 'Consumo Humano y No Consumo Humano'];
  if (isEmpty(get('tipo_consumo'))) {
    errors.push(err('L', 'tipo_consumo', 'Campo obligatorio'));
  } else if (!validConsumo.includes(getStr('tipo_consumo'))) {
    errors.push(err('L', 'tipo_consumo', 'Valor no válido para tipo de consumo'));
  }

  // M, N, O — required
  for (const [f, col] of [['codigo_producto', 'M'], ['nombre_comun', 'N'], ['linea_proceso', 'O']]) {
    if (isEmpty(get(f))) errors.push(err(col, f, 'Campo obligatorio'));
  }

  // P: Fecha elaboración — required, range, > fecha_extraccion
  let fechaElaboracion: Date | null = null;
  if (isEmpty(get('fecha_elaboracion'))) {
    errors.push(err('P', 'fecha_elaboracion', 'Campo obligatorio'));
  } else {
    fechaElaboracion = validateDateRange(get('fecha_elaboracion'), 'P', 'fecha_elaboracion', minDate, maxDate, errors);
    if (fechaElaboracion && fechaExtraccion && fechaElaboracion <= fechaExtraccion) {
      errors.push(err('P', 'fecha_elaboracion', 'Debe ser posterior a la fecha de extracción'));
    }
  }

  // Q: Fecha inicio verificación — required when tipo_control is verificación
  let fechaInicioVer: Date | null = null;
  if (VERIFICACION_TYPES.has(tipoControl)) {
    if (isEmpty(get('fecha_inicio_verificacion'))) {
      errors.push(err('Q', 'fecha_inicio_verificacion', 'Obligatorio para Verificación Sernapesca/Periódica'));
    } else {
      fechaInicioVer = validateDateRange(get('fecha_inicio_verificacion'), 'Q', 'fecha_inicio_verificacion', minDate, maxDate, errors);
      if (fechaInicioVer && fechaElaboracion && fechaInicioVer > fechaElaboracion) {
        errors.push(err('Q', 'fecha_inicio_verificacion', 'Debe ser menor o igual a fecha de elaboración'));
      }
    }
  }

  // R: Fecha fin verificación — required when verificación
  let fechaFinVer: Date | null = null;
  if (VERIFICACION_TYPES.has(tipoControl)) {
    if (isEmpty(get('fecha_fin_verificacion'))) {
      errors.push(err('R', 'fecha_fin_verificacion', 'Obligatorio para Verificación Sernapesca/Periódica'));
    } else {
      fechaFinVer = validateDateRange(get('fecha_fin_verificacion'), 'R', 'fecha_fin_verificacion', minDate, maxDatePlus1Y, errors);
      if (fechaFinVer && fechaInicioVer && fechaFinVer < fechaInicioVer) {
        errors.push(err('R', 'fecha_fin_verificacion', 'Debe ser mayor o igual a fecha de inicio de verificación'));
      }
      if (fechaFinVer && fechaElaboracion && fechaFinVer < fechaElaboracion) {
        errors.push(err('R', 'fecha_fin_verificacion', 'Debe ser mayor o igual a la fecha de elaboración'));
      }
    }
  }

  // S: Nombre entidad de muestreo — required
  if (isEmpty(get('nombre_entidad_muestreo'))) {
    errors.push(err('S', 'nombre_entidad_muestreo', 'Campo obligatorio'));
  }

  // T: Fecha muestreo — required
  let fechaMuestreo: Date | null = null;
  if (isEmpty(get('fecha_muestreo'))) {
    errors.push(err('T', 'fecha_muestreo', 'Campo obligatorio'));
  } else {
    fechaMuestreo = validateDateRange(get('fecha_muestreo'), 'T', 'fecha_muestreo', minDate, maxDate, errors);
    if (fechaMuestreo && fechaElaboracion && fechaMuestreo < fechaElaboracion) {
      errors.push(err('T', 'fecha_muestreo', 'Debe ser mayor a la fecha de elaboración'));
    }
    if (fechaMuestreo && fechaInicioVer && fechaMuestreo <= fechaInicioVer) {
      errors.push(err('T', 'fecha_muestreo', 'Debe ser mayor que la fecha de inicio de verificación'));
    }
  }

  // U: Fecha envío muestras — required, >= T
  let fechaEnvio: Date | null = null;
  if (isEmpty(get('fecha_envio_muestras'))) {
    errors.push(err('U', 'fecha_envio_muestras', 'Campo obligatorio'));
  } else {
    fechaEnvio = validateDateRange(get('fecha_envio_muestras'), 'U', 'fecha_envio_muestras', minDate, maxDate, errors);
    if (fechaEnvio && fechaMuestreo && fechaEnvio < fechaMuestreo) {
      errors.push(err('U', 'fecha_envio_muestras', 'Debe ser mayor o igual a la fecha de muestreo'));
    }
  }

  // V: Fecha recepción muestras — required, >= U
  let fechaRecepcion: Date | null = null;
  if (isEmpty(get('fecha_recepcion_muestras'))) {
    errors.push(err('V', 'fecha_recepcion_muestras', 'Campo obligatorio'));
  } else {
    fechaRecepcion = validateDateRange(get('fecha_recepcion_muestras'), 'V', 'fecha_recepcion_muestras', minDate, maxDate, errors);
    if (fechaRecepcion && fechaEnvio && fechaRecepcion < fechaEnvio) {
      errors.push(err('V', 'fecha_recepcion_muestras', 'Debe ser mayor o igual a fecha de envío'));
    }
  }

  // W, X, Y, Z, AA — required
  for (const [f, col] of [['codigo_muestra', 'W'], ['tipo_analisis', 'X'], ['analisis_solicitado', 'Y'], ['valor_obtenido', 'Z'], ['unidad_medida', 'AA']]) {
    if (isEmpty(get(f))) errors.push(err(col, f, 'Campo obligatorio'));
  }

  // AB: Fecha inicio análisis — required, >= V
  let fechaInicioAnalisis: Date | null = null;
  if (isEmpty(get('fecha_inicio_analisis'))) {
    errors.push(err('AB', 'fecha_inicio_analisis', 'Campo obligatorio'));
  } else {
    fechaInicioAnalisis = validateDateRange(get('fecha_inicio_analisis'), 'AB', 'fecha_inicio_analisis', minDate, maxDate, errors);
    if (fechaInicioAnalisis && fechaRecepcion && fechaInicioAnalisis < fechaRecepcion) {
      errors.push(err('AB', 'fecha_inicio_analisis', 'Debe ser mayor o igual a fecha de recepción'));
    }
  }

  // AC: Fecha obtención resultados — required, >= AB
  let fechaObtencion: Date | null = null;
  if (isEmpty(get('fecha_obtencion_resultados'))) {
    errors.push(err('AC', 'fecha_obtencion_resultados', 'Campo obligatorio'));
  } else {
    fechaObtencion = validateDateRange(get('fecha_obtencion_resultados'), 'AC', 'fecha_obtencion_resultados', minDate, maxDate, errors);
    if (fechaObtencion && fechaInicioAnalisis && fechaObtencion < fechaInicioAnalisis) {
      errors.push(err('AC', 'fecha_obtencion_resultados', 'Debe ser mayor o igual a fecha de inicio de análisis'));
    }
  }

  // AD: Fecha emisión informe — required, >= AC
  let fechaEmision: Date | null = null;
  if (isEmpty(get('fecha_emision_informe'))) {
    errors.push(err('AD', 'fecha_emision_informe', 'Campo obligatorio'));
  } else {
    fechaEmision = validateDateRange(get('fecha_emision_informe'), 'AD', 'fecha_emision_informe', minDate, maxDate, errors);
    if (fechaEmision && fechaObtencion && fechaEmision < fechaObtencion) {
      errors.push(err('AD', 'fecha_emision_informe', 'Debe ser mayor o igual a fecha de obtención de resultados'));
    }
  }

  // AE: Externalización — optional
  // AF: Nombre lab externalización — required if AE filled
  if (!isEmpty(get('externalizacion')) && isEmpty(get('nombre_lab_externalizacion'))) {
    errors.push(err('AF', 'nombre_lab_externalizacion', 'Obligatorio cuando se indica externalización'));
  }

  return {
    rowNumber,
    isValid: errors.length === 0,
    data: row,
    errors,
  };
}
