import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardLaboratorio {
  id_laboratorio: number;
  codigo: string;
  nombre: string;
  tipo_laboratorio: string;
  activo: boolean;
}

export interface DashboardReporte {
  id: number;
  tipo: string;
  anio: number;
  mes: number;
  nombreLaboratorio: string;
  nombreArchivo: string;
  fechaEnvio: string;
  totalRegistros: number;
  periodoId: number | null;
}

export interface DashboardData {
  totalLaboratorios: number;
  totalPlantillas: number;
  periodosAbiertos: number;
  totalReportes: number;
  reportesRecientes: DashboardReporte[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  cargar(): Observable<{
    laboratorios: DashboardLaboratorio[];
    plantillas: any[];
    periodos: any[];
    reportes: DashboardReporte[];
  }> {
    return forkJoin({
      laboratorios: this.http.get<DashboardLaboratorio[]>(`${environment.javaApi}/laboratorios`)
                        .pipe(catchError(() => of([]))),
      plantillas:   this.http.get<any[]>(`${environment.javaApi}/plantillas`)
                        .pipe(catchError(() => of([]))),
      periodos:     this.http.get<any[]>(`${environment.javaApi}/periodos`)
                        .pipe(catchError(() => of([]))),
      reportes:     this.http.get<DashboardReporte[]>(`${environment.javaApi}/reportes`)
                        .pipe(catchError(() => of([]))),
    });
  }
}
