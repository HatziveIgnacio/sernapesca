import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlantillasService, PlantillaMetadata } from './plantillas.service';

@Component({
  selector: 'app-plantillas',
  imports: [CommonModule, FormsModule],
  templateUrl: './plantillas.html',
  styleUrl: './plantillas.css',
})
export class Plantillas {
  private readonly svc = inject(PlantillasService);
  private readonly cdr = inject(ChangeDetectorRef);

  plantillas: PlantillaMetadata[] = [];
  isLoading = false;
  isUploading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  // Formulario de subida
  selectedFile: File | null = null;
  anio = new Date().getFullYear();
  mes = new Date().getMonth() + 1;
  tipo: 'RRA' | 'RRA_FAR' = 'RRA';

  readonly meses = [
    'Enero','Febrero','Marzo','Abril','Mayo','Junio',
    'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'
  ];

  /** Año máximo seleccionable: el año actual */
  readonly anioActual = new Date().getFullYear();
  /** Mes actual (1-12) */
  readonly mesActual = new Date().getMonth() + 1;

  /**
   * Retorna los índices de mes (1-12) permitidos para el año seleccionado.
   * Si es el año actual, solo hasta el mes corriente.
   */
  get mesesPermitidos(): number[] {
    const max = this.anio === this.anioActual ? this.mesActual : 12;
    return Array.from({ length: max }, (_, i) => i + 1);
  }

  /** Ajusta el mes si al cambiar el año queda fuera de rango */
  onAnioChange(valor: string) {
    this.anio = Number(valor);
    const max = this.anio === this.anioActual ? this.mesActual : 12;
    if (this.mes > max) this.mes = max;
  }

  ngOnInit() {
    this.cargarPlantillas();
  }

  cargarPlantillas() {
    this.isLoading = true;
    this.svc.listar().subscribe({
      next: (data) => {
        this.plantillas = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'No se pudo conectar con el backend Java. Asegúrese de que esté corriendo en :8080';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onFileInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!file.name.match(/\.(xlsx|xlsm)$/i)) {
      this.errorMessage = 'Solo se permiten archivos .xlsx o .xlsm';
      return;
    }
    this.selectedFile = file;
    this.errorMessage = null;
    input.value = '';
  }

  upload() {
    if (!this.selectedFile) return;
    this.isUploading = true;
    this.errorMessage = null;
    this.successMessage = null;

    this.svc.upload(this.selectedFile, Number(this.anio), Number(this.mes), this.tipo).subscribe({
      next: () => {
        this.successMessage = `Plantilla ${this.tipo} ${this.meses[this.mes - 1]} ${this.anio} subida correctamente.`;
        this.selectedFile = null;
        this.isUploading = false;
        this.cargarPlantillas();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Error al subir la plantilla';
        this.isUploading = false;
        this.cdr.detectChanges();
      }
    });
  }

  descargar(plantilla: PlantillaMetadata) {
    window.open(this.svc.getDownloadUrl(plantilla.id), '_blank');
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  getNombreMes(mes: number): string {
    return this.meses[mes - 1] ?? String(mes);
  }
}
