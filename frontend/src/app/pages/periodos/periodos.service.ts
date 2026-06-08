import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type EstadoPeriodo = 'ABIERTO' | 'CERRADO';
export type TipoPeriodo = 'RRA' | 'RRA_FAR';

export interface PeriodoDto {
  id: number;
  anio: number;
  mes: number;
  tipo: TipoPeriodo;
  plantillaId: string | null;
  estado: EstadoPeriodo;
  fechaCreacion: string;
  fechaCierre: string | null;
  consolidadoSolicitado: boolean;
}

export interface CrearPeriodoRequest {
  anio: number;
  mes: number;
  tipo: TipoPeriodo;
  plantillaId?: string | null;
}

export interface CerrarPeriodoResponse {
  periodo: PeriodoDto;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class PeriodosService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.javaApi}/periodos`;

  listar(): Observable<PeriodoDto[]> {
    return this.http.get<PeriodoDto[]>(this.api);
  }

  crear(req: CrearPeriodoRequest): Observable<PeriodoDto> {
    return this.http.post<PeriodoDto>(this.api, req);
  }

  cerrar(id: number): Observable<CerrarPeriodoResponse> {
    return this.http.put<CerrarPeriodoResponse>(`${this.api}/${id}/cerrar`, {});
  }

  vincularPlantilla(id: number, plantillaId: string): Observable<PeriodoDto> {
    return this.http.put<PeriodoDto>(`${this.api}/${id}/plantilla`, { plantillaId });
  }

  /**
   * Descarga el consolidado del período: un Excel con todos los reportes
   * validados apilados. Devuelve el blob (404 si no hay reportes).
   */
  descargarConsolidado(tipo: TipoPeriodo, anio: number, mes: number): Observable<Blob> {
    return this.http.get(
      `${environment.javaApi}/consolidados/${tipo}/download?anio=${anio}&mes=${mes}`,
      { responseType: 'blob' }
    );
  }
}
