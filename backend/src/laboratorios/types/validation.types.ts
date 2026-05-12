export type TemplateType = 'RRA' | 'RRA_FAR';

export interface FieldError {
  column: string;
  field: string;
  message: string;
}

// Tipo interno que usan los validadores
export interface ValidatedRow {
  rowNumber: number;
  isValid: boolean;
  data: Record<string, unknown>;
  errors: FieldError[];
}

// Tipo que devuelve la API al frontend
export interface RowResult {
  rowNumber: number;
  isValid: boolean;
  display: {
    codigoMuestra: string;
    nInforme: string;
    analisis: string;
    valorObtenido: string;
    fechaMuestreo: string;
  };
  errors: FieldError[];
}

export interface ValidationResult {
  templateType: TemplateType;
  totalRows: number;
  validRows: number;
  errorRows: number;
  rows: RowResult[];
}
