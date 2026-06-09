import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TemplateType = 'RRA' | 'RRA_FAR';

export interface FieldError {
  column: string;
  field: string;
  message: string;
}

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

export interface FinalizarResult {
  reporteId: number;
  tipo: TemplateType;
  anio: number;
  mes: number;
  nombreLaboratorio: string;
  totalRegistros: number;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class LaboratoriosService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = `${environment.javaApi}/laboratorios`;

  /**
   * Sube el reporte del laboratorio para validar. anio/mes son opcionales:
   * si se omiten, el backend valida contra el período ABIERTO más reciente.
   */
  uploadFile(file: File, templateType: TemplateType, anio?: number, mes?: number): Observable<ValidationResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('templateType', templateType);
    if (anio != null) form.append('anio', String(anio));
    if (mes != null) form.append('mes', String(mes));
    return this.http.post<ValidationResult>(`${this.apiBase}/upload`, form);
  }

  /**
   * Finaliza y sube el reporte validado a SERNAPESCA. El backend lo re-valida y,
   * si no hay errores, persiste los registros para el consolidado del período.
   */
  finalizar(file: File, templateType: TemplateType, anio?: number, mes?: number): Observable<FinalizarResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('templateType', templateType);
    if (anio != null) form.append('anio', String(anio));
    if (mes != null) form.append('mes', String(mes));
    return this.http.post<FinalizarResult>(`${environment.javaApi}/reportes/finalizar`, form);
  }

  downloadPlantilla(templateType: TemplateType): Observable<Blob> {
    return this.http.get(
      `${environment.javaApi}/plantillas/latest/${templateType}`,
      { responseType: 'blob' }
    );
  }
}
