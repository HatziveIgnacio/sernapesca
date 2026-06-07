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

@Injectable({ providedIn: 'root' })
export class LaboratoriosService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = `${environment.nestApi}/laboratorios`;

  uploadFile(file: File, templateType: TemplateType): Observable<ValidationResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('templateType', templateType);
    return this.http.post<ValidationResult>(`${this.apiBase}/upload`, form);
  }

  downloadPlantilla(templateType: TemplateType): Observable<Blob> {
    return this.http.get(
      `${environment.javaApi}/plantillas/latest/${templateType}`,
      { responseType: 'blob' }
    );
  }
}
