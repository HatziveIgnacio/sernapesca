import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PeriodosService, PeriodoDto, TipoPeriodo } from './periodos.service';
import { PlantillasService, PlantillaMetadata } from '../plantillas/plantillas.service';

@Component({
  selector: 'app-periodos',
  imports: [CommonModule, FormsModule],
  templateUrl: './periodos.html',
  styleUrl: './periodos.css',
})
export class Periodos {
  private readonly periodosSvc = inject(PeriodosService);
  private readonly plantillasSvc = inject(PlantillasService);
  private readonly cdr = inject(ChangeDetectorRef);

  periodos: PeriodoDto[] = [];
  plantillas: PlantillaMetadata[] = [];
  isLoading = false;
  isCreating = false;
  errorMessage = '';
  successMessage = '';

  // Form crear período
  readonly anioActual = new Date().getFullYear();
  readonly mesActual = new Date().getMonth() + 1;
  anio = this.anioActual;
  mes = this.mesActual;
  tipo: TipoPeriodo = 'RRA';
  plantillaId: string = '';

  readonly meses = [
    'Enero','Febrero','Marzo','Abril','Mayo','Junio',
    'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'
  ];

  get mesesPermitidos(): number[] {
    const max = this.anio === this.anioActual ? this.mesActual : 12;
    return Array.from({ length: max }, (_, i) => i + 1);
  }

  /** Plantillas filtradas según el tipo elegido */
  get plantillasFiltradas(): PlantillaMetadata[] {
    return this.plantillas.filter(p => p.tipo === this.tipo);
  }

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.isLoading = true;
    this.periodosSvc.listar().subscribe({
      next: data => {
        this.periodos = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorMessage = this.extractError(err, 'No se pudo cargar los períodos. ¿Backend Java en :8080?');
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });

    this.plantillasSvc.listar().subscribe({
      next: data => {
        this.plantillas = data;
        this.cdr.detectChanges();
      },
      error: () => { /* el error de plantillas no bloquea esta vista */ }
    });
  }

  onTipoChange() {
    this.plantillaId = '';
  }

  onAnioChange(valor: string) {
    this.anio = Number(valor);
    const max = this.anio === this.anioActual ? this.mesActual : 12;
    if (this.mes > max) this.mes = max;
  }

  crear() {
    this.isCreating = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.periodosSvc.crear({
      anio: Number(this.anio),
      mes: Number(this.mes),
      tipo: this.tipo,
      plantillaId: this.plantillaId || null,
    }).subscribe({
      next: p => {
        this.successMessage = `Período ${p.tipo} ${this.meses[p.mes - 1]} ${p.anio} creado.`;
        this.isCreating = false;
        this.cargar();
      },
      error: err => {
        this.errorMessage = this.extractError(err, 'No se pudo crear el período');
        this.isCreating = false;
        this.cdr.detectChanges();
      }
    });
  }

  cerrar(p: PeriodoDto) {
    const periodoLabel = `${p.tipo} ${this.meses[p.mes - 1]} ${p.anio}`;
    if (!confirm(`¿Cerrar el período ${periodoLabel}?\n\nUna vez cerrado:\n• Los laboratorios no podrán subir más reportes\n• Se gatilla la generación del consolidado\n• La acción no se puede deshacer`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.periodosSvc.cerrar(p.id).subscribe({
      next: res => {
        this.successMessage = `${res.mensaje} (${periodoLabel})`;
        this.cargar();
      },
      error: err => {
        this.errorMessage = this.extractError(err, 'No se pudo cerrar el período');
        this.cdr.detectChanges();
      }
    });
  }

  nombreMes(mes: number): string {
    return this.meses[mes - 1] ?? String(mes);
  }

  plantillaLabel(plantillaId: string | null): string {
    if (!plantillaId) return '— sin vincular —';
    const p = this.plantillas.find(x => x.id === plantillaId);
    if (!p) return `(plantilla ${plantillaId.slice(0, 8)}…)`;
    return `${p.tipo} ${this.meses[p.mes - 1]} ${p.anio}`;
  }

  private extractError(err: any, fallback: string): string {
    return err?.error?.message ?? err?.message ?? fallback;
  }
}
