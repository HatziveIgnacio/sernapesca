import { FieldError, ValidatedRow } from '../types/validation.types';
import { parseDate, today, addYears, dateOnly } from './date.utils';

export const RRA_FAR_COLUMNS: { col: string; field: string }[] = [
  { col: 'A', field: 'nombre_laboratorio' },
  { col: 'B', field: 'tipo_laboratorio' },
  { col: 'C', field: 'n_informe' },
  { col: 'D', field: 'tipo_formulario' },
  { col: 'E', field: 'n_formulario' },
  { col: 'F', field: 'tipo_control' },
  { col: 'G', field: 'id_siscomex' },
  { col: 'H', field: 'codigo_establecimiento' },
  { col: 'I', field: 'razon_social' },
  { col: 'J', field: 'codigo_centro_cultivo' },
  { col: 'K', field: 'nombre_centro_cultivo' },
  { col: 'L', field: 'jaula' },
  { col: 'M', field: 'tipo_consumo' },
  { col: 'N', field: 'codigo_producto' },
  { col: 'O', field: 'nombre_comun' },
  { col: 'P', field: 'linea_proceso' },
  { col: 'Q', field: 'fecha_elaboracion' },
  { col: 'R', field: 'fecha_inicio_verificacion' },
  { col: 'S', field: 'fecha_fin_verificacion' },
  { col: 'T', field: 'nombre_entidad_muestreo' },
  { col: 'U', field: 'fecha_muestreo' },
  { col: 'V', field: 'fecha_envio_muestras' },
  { col: 'W', field: 'fecha_recepcion_muestras' },
  { col: 'X', field: 'codigo_muestra' },
  { col: 'Y', field: 'tipo_analisis' },
  { col: 'Z', field: 'analisis_solicitado' },
  { col: 'AA', field: 'valor_obtenido' },
  { col: 'AB', field: 'unidad_medida' },
  { col: 'AC', field: 'fecha_inicio_analisis' },
  { col: 'AD', field: 'fecha_obtencion_resultados' },
  { col: 'AE', field: 'fecha_emision_informe' },
  { col: 'AF', field: 'externalizacion' },
  { col: 'AG', field: 'nombre_lab_externalizacion' },
  { col: 'AH', field: 'anula_reemplaza' },
];

const VERIFICACION_TYPES = new Set(['Verificación Sernapesca', 'Verificación periódica PAC']);
const CENTRO_CULTIVO_CONTROLS = new Set([
  'Control Mensual',
  'Control de Sustancias Prohibidas y No autorizadas',
  'Control Precosecha',
]);
const FEMPAC_SMAE = new Set(['FEMPAC', 'SMAE']);

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

export function validateRraFarRow(
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

  // A, B, C, E — always required
  for (const [f, col] of [['nombre_laboratorio', 'A'], ['tipo_laboratorio', 'B'], ['n_informe', 'C'], ['n_formulario', 'E']]) {
    if (isEmpty(get(f))) errors.push(err(col, f, 'Campo obligatorio'));
  }

  // D: tipo_formulario
  const tipoFormulario = getStr('tipo_formulario');
  const validFormularios = ['FEMPAC', 'SMAE', 'Formulario de envío de muestras e informe de muestreo', 'Solicitud de muestreo de cosecha (precosecha)'];
  if (isEmpty(tipoFormulario)) {
    errors.push(err('D', 'tipo_formulario', 'Campo obligatorio'));
  } else if (!validFormularios.includes(tipoFormulario)) {
    errors.push(err('D', 'tipo_formulario', 'Valor no válido para tipo de formulario'));
  }
  const isFempacSmae = FEMPAC_SMAE.has(tipoFormulario);

  // F: tipo_control
  const tipoControl = getStr('tipo_control');
  const validControls = [
    'Verificación Sernapesca',
    'Verificación periódica PAC',
    'Acción correctiva debido a un desfavorable anterior',
    'Control de producto final',
    'Control Mensual',
    'Control de Sustancias Prohibidas y No autorizadas',
    'Control Precosecha',
  ];
  if (isEmpty(tipoControl)) {
    errors.push(err('F', 'tipo_control', 'Campo obligatorio'));
  } else if (!validControls.includes(tipoControl)) {
    errors.push(err('F', 'tipo_control', 'Valor no válido para tipo de control'));
  }

  // G: ID SISCOMEX — required when FEMPAC
  if (tipoFormulario === 'FEMPAC' && isEmpty(get('id_siscomex'))) {
    errors.push(err('G', 'id_siscomex', 'Obligatorio cuando el formulario es FEMPAC'));
  }

  // H, I: required only for FEMPAC/SMAE
  if (isFempacSmae) {
    if (isEmpty(get('codigo_establecimiento'))) errors.push(err('H', 'codigo_establecimiento', 'Obligatorio para FEMPAC/SMAE'));
    if (isEmpty(get('razon_social'))) errors.push(err('I', 'razon_social', 'Obligatorio para FEMPAC/SMAE'));
  }

  // J, K, L: required for centro cultivo control types
  const requiresCentro = CENTRO_CULTIVO_CONTROLS.has(tipoControl);
  if (requiresCentro) {
    if (isEmpty(get('codigo_centro_cultivo'))) errors.push(err('J', 'codigo_centro_cultivo', 'Obligatorio para este tipo de control'));
    if (isEmpty(get('jaula'))) errors.push(err('L', 'jaula', 'Obligatorio para este tipo de control'));
  }
  if (!isEmpty(get('codigo_centro_cultivo')) && isEmpty(get('nombre_centro_cultivo'))) {
    errors.push(err('K', 'nombre_centro_cultivo', 'Obligatorio cuando se indica código de centro de cultivo'));
  }

  // M: tipo_consumo — required only for FEMPAC/SMAE
  if (isFempacSmae) {
    const validConsumo = ['Consumo Humano', 'No Consumo Humano', 'Consumo Humano y No Consumo Humano'];
    if (isEmpty(get('tipo_consumo'))) {
      errors.push(err('M', 'tipo_consumo', 'Obligatorio para FEMPAC/SMAE'));
    } else if (!validConsumo.includes(getStr('tipo_consumo'))) {
      errors.push(err('M', 'tipo_consumo', 'Valor no válido para tipo de consumo'));
    }
  }

  // N, P: required only for FEMPAC/SMAE
  if (isFempacSmae) {
    if (isEmpty(get('codigo_producto'))) errors.push(err('N', 'codigo_producto', 'Obligatorio para FEMPAC/SMAE'));
    if (isEmpty(get('linea_proceso'))) errors.push(err('P', 'linea_proceso', 'Obligatorio para FEMPAC/SMAE'));
  }

  // O: nombre_comun — always required
  if (isEmpty(get('nombre_comun'))) errors.push(err('O', 'nombre_comun', 'Campo obligatorio'));

  // Q: fecha_elaboracion — required only for FEMPAC/SMAE
  let fechaElaboracion: Date | null = null;
  if (isFempacSmae) {
    if (isEmpty(get('fecha_elaboracion'))) {
      errors.push(err('Q', 'fecha_elaboracion', 'Obligatorio para FEMPAC/SMAE'));
    } else {
      fechaElaboracion = validateDateRange(get('fecha_elaboracion'), 'Q', 'fecha_elaboracion', minDate, maxDate, errors);
    }
  }

  // R: Fecha inicio verificación
  let fechaInicioVer: Date | null = null;
  if (VERIFICACION_TYPES.has(tipoControl)) {
    if (isEmpty(get('fecha_inicio_verificacion'))) {
      errors.push(err('R', 'fecha_inicio_verificacion', 'Obligatorio para Verificación Sernapesca/Periódica'));
    } else {
      fechaInicioVer = validateDateRange(get('fecha_inicio_verificacion'), 'R', 'fecha_inicio_verificacion', minDate, maxDate, errors);
      if (fechaInicioVer && fechaElaboracion && fechaInicioVer > fechaElaboracion) {
        errors.push(err('R', 'fecha_inicio_verificacion', 'Debe ser menor o igual a fecha de elaboración'));
      }
    }
  }

  // S: Fecha fin verificación
  let fechaFinVer: Date | null = null;
  if (VERIFICACION_TYPES.has(tipoControl)) {
    if (isEmpty(get('fecha_fin_verificacion'))) {
      errors.push(err('S', 'fecha_fin_verificacion', 'Obligatorio para Verificación Sernapesca/Periódica'));
    } else {
      fechaFinVer = validateDateRange(get('fecha_fin_verificacion'), 'S', 'fecha_fin_verificacion', minDate, maxDatePlus1Y, errors);
      if (fechaFinVer && fechaInicioVer && fechaFinVer < fechaInicioVer) {
        errors.push(err('S', 'fecha_fin_verificacion', 'Debe ser mayor o igual a fecha de inicio de verificación'));
      }
      if (fechaFinVer && fechaElaboracion && fechaFinVer <= fechaElaboracion) {
        errors.push(err('S', 'fecha_fin_verificacion', 'Debe ser mayor a la fecha de elaboración'));
      }
    }
  }

  // T: Nombre entidad muestreo — required
  if (isEmpty(get('nombre_entidad_muestreo'))) errors.push(err('T', 'nombre_entidad_muestreo', 'Campo obligatorio'));

  // U: Fecha muestreo — required
  let fechaMuestreo: Date | null = null;
  if (isEmpty(get('fecha_muestreo'))) {
    errors.push(err('U', 'fecha_muestreo', 'Campo obligatorio'));
  } else {
    fechaMuestreo = validateDateRange(get('fecha_muestreo'), 'U', 'fecha_muestreo', minDate, maxDate, errors);
    if (fechaMuestreo && fechaElaboracion && fechaMuestreo <= fechaElaboracion) {
      errors.push(err('U', 'fecha_muestreo', 'Debe ser mayor a la fecha de elaboración'));
    }
    if (fechaMuestreo && fechaInicioVer && fechaMuestreo <= fechaInicioVer) {
      errors.push(err('U', 'fecha_muestreo', 'Debe ser mayor que la fecha de inicio de verificación'));
    }
  }

  // V: Fecha envío muestras — required, >= U
  let fechaEnvio: Date | null = null;
  if (isEmpty(get('fecha_envio_muestras'))) {
    errors.push(err('V', 'fecha_envio_muestras', 'Campo obligatorio'));
  } else {
    fechaEnvio = validateDateRange(get('fecha_envio_muestras'), 'V', 'fecha_envio_muestras', minDate, maxDate, errors);
    if (fechaEnvio && fechaMuestreo && fechaEnvio < fechaMuestreo) {
      errors.push(err('V', 'fecha_envio_muestras', 'Debe ser mayor o igual a la fecha de muestreo'));
    }
  }

  // W: Fecha recepción muestras — required, >= V
  let fechaRecepcion: Date | null = null;
  if (isEmpty(get('fecha_recepcion_muestras'))) {
    errors.push(err('W', 'fecha_recepcion_muestras', 'Campo obligatorio'));
  } else {
    fechaRecepcion = validateDateRange(get('fecha_recepcion_muestras'), 'W', 'fecha_recepcion_muestras', minDate, maxDate, errors);
    if (fechaRecepcion && fechaEnvio && fechaRecepcion < fechaEnvio) {
      errors.push(err('W', 'fecha_recepcion_muestras', 'Debe ser mayor o igual a fecha de envío'));
    }
  }

  // X, Y, Z, AA — required
  for (const [f, col] of [['codigo_muestra', 'X'], ['tipo_analisis', 'Y'], ['analisis_solicitado', 'Z'], ['valor_obtenido', 'AA'], ['unidad_medida', 'AB']]) {
    if (isEmpty(get(f))) errors.push(err(col, f, 'Campo obligatorio'));
  }

  // AC: Fecha inicio análisis — required, >= W
  let fechaInicioAnalisis: Date | null = null;
  if (isEmpty(get('fecha_inicio_analisis'))) {
    errors.push(err('AC', 'fecha_inicio_analisis', 'Campo obligatorio'));
  } else {
    fechaInicioAnalisis = validateDateRange(get('fecha_inicio_analisis'), 'AC', 'fecha_inicio_analisis', minDate, maxDate, errors);
    if (fechaInicioAnalisis && fechaRecepcion && fechaInicioAnalisis < fechaRecepcion) {
      errors.push(err('AC', 'fecha_inicio_analisis', 'Debe ser mayor o igual a fecha de recepción'));
    }
  }

  // AD: Fecha obtención resultados — required, >= AC
  let fechaObtencion: Date | null = null;
  if (isEmpty(get('fecha_obtencion_resultados'))) {
    errors.push(err('AD', 'fecha_obtencion_resultados', 'Campo obligatorio'));
  } else {
    fechaObtencion = validateDateRange(get('fecha_obtencion_resultados'), 'AD', 'fecha_obtencion_resultados', minDate, maxDate, errors);
    if (fechaObtencion && fechaInicioAnalisis && fechaObtencion < fechaInicioAnalisis) {
      errors.push(err('AD', 'fecha_obtencion_resultados', 'Debe ser mayor o igual a fecha de inicio de análisis'));
    }
  }

  // AE: Fecha emisión informe — required, >= AD
  let fechaEmision: Date | null = null;
  if (isEmpty(get('fecha_emision_informe'))) {
    errors.push(err('AE', 'fecha_emision_informe', 'Campo obligatorio'));
  } else {
    fechaEmision = validateDateRange(get('fecha_emision_informe'), 'AE', 'fecha_emision_informe', minDate, maxDate, errors);
    if (fechaEmision && fechaObtencion && fechaEmision < fechaObtencion) {
      errors.push(err('AE', 'fecha_emision_informe', 'Debe ser mayor o igual a fecha de obtención de resultados'));
    }
  }

  // AF: Externalización — optional
  // AG: required if AF filled
  if (!isEmpty(get('externalizacion')) && isEmpty(get('nombre_lab_externalizacion'))) {
    errors.push(err('AG', 'nombre_lab_externalizacion', 'Obligatorio cuando se indica externalización'));
  }

  return {
    rowNumber,
    isValid: errors.length === 0,
    data: row,
    errors,
  };
}
