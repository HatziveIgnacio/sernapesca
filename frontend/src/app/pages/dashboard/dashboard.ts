import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService, DashboardReporte } from './dashboard.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly svc = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);

  isLoading = true;
  errorMessage = '';

  // KPIs
  totalLaboratorios = 0;
  totalPlantillas = 0;
  periodosAbiertos = 0;
  totalReportes = 0;

  // Tabla
  reportesRecientes: DashboardReporte[] = [];

  readonly meses = [
    'Enero','Febrero','Marzo','Abril','Mayo','Junio',
    'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'
  ];

  ngOnInit(): void {
    this.svc.cargar().subscribe({
      next: ({ laboratorios, plantillas, periodos, reportes }) => {
        this.totalLaboratorios = laboratorios.length;
        this.totalPlantillas   = plantillas.length;
        this.periodosAbiertos  = periodos.filter((p: any) => p.estado === 'ABIERTO').length;
        this.totalReportes     = reportes.length;
        this.reportesRecientes = reportes.slice(0, 6);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'No se pudo conectar con el backend. ¿Está corriendo en :8080?';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  nombreMes(mes: number): string {
    return this.meses[mes - 1] ?? String(mes);
  }
}
