import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LaboratoriosService, TemplateType, ValidationResult, RowResult } from './laboratorios.service';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-laboratorios',
  imports: [CommonModule],
  templateUrl: './laboratorios.html',
  styleUrl: './laboratorios.css',
})
export class Laboratorios {
  private readonly svc = inject(LaboratoriosService);
  private readonly cdr = inject(ChangeDetectorRef);

  templateType: TemplateType = 'RRA';
  currentStep = 1;
  isDragOver = false;
  isLoading = false;
  selectedFile: File | null = null;
  validationResult: ValidationResult | null = null;
  errorMessage: string | null = null;
  selectedErrorRow: RowResult | null = null;

  private searchTerm = '';
  currentPage = 1;
  readonly pageSize = PAGE_SIZE;

  private get filteredRows(): RowResult[] {
    if (!this.validationResult) return [];
    const term = this.searchTerm.toLowerCase();
    if (!term) return this.validationResult.rows;
    return this.validationResult.rows.filter((row) => {
      const d = row.display;
      const displayStr = `${d.codigoMuestra} ${d.nInforme} ${d.analisis} ${d.valorObtenido}`.toLowerCase();
      const errs = row.errors.map((e) => e.message + e.column).join(' ').toLowerCase();
      return displayStr.includes(term) || errs.includes(term);
    });
  }

  get pagedRows(): RowResult[] {
    const start = (this.currentPage - 1) * PAGE_SIZE;
    return this.filteredRows.slice(start, start + PAGE_SIZE);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / PAGE_SIZE));
  }

  get paginationLabel(): string {
    const total = this.filteredRows.length;
    if (total === 0) return '0';
    const from = (this.currentPage - 1) * PAGE_SIZE + 1;
    const to = Math.min(this.currentPage * PAGE_SIZE, total);
    return `${from}–${to} de ${total}`;
  }

  get visiblePages(): number[] {
    const total = this.totalPages;
    const cur = this.currentPage;
    const pages: number[] = [];
    for (let p = 1; p <= total; p++) {
      if (p === 1 || p === total || (p >= cur - 1 && p <= cur + 1)) {
        pages.push(p);
      } else if (p === cur - 2 || p === cur + 2) {
        pages.push(-1);
      }
    }
    return pages.filter((v, i) => v !== -1 || pages[i - 1] !== -1);
  }

  onTemplateChange(value: string) {
    this.templateType = value as TemplateType;
    this.resetUpload();
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave() {
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.selectFile(file);
  }

  onFileInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.selectFile(file);
    input.value = '';
  }

  private selectFile(file: File) {
    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    if (!['xlsx', 'xlsm', 'csv'].includes(ext)) {
      this.errorMessage = 'Solo se permiten archivos .xlsx, .xlsm o .csv';
      return;
    }
    if (file.size > 25 * 1024 * 1024) {
      this.errorMessage = 'El archivo supera el límite de 25MB';
      return;
    }
    this.errorMessage = null;
    this.selectedFile = file;
    this.validationResult = null;
    this.currentStep = 1;
  }

  validate() {
    if (!this.selectedFile) return;
    this.isLoading = true;
    this.currentStep = 2;
    this.errorMessage = null;

    this.svc.uploadFile(this.selectedFile, this.templateType).subscribe({
      next: (result) => {
        this.validationResult = result;
        this.currentPage = 1;
        this.isLoading = false;
        this.currentStep = result.errorRows > 0 ? 3 : 4;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.currentStep = 1;
        const msg = err?.error?.message ?? 'Error al procesar el archivo';
        this.errorMessage = Array.isArray(msg) ? msg.join(', ') : msg;
        this.cdr.detectChanges();
      },
    });
  }

  resetUpload() {
    this.selectedFile = null;
    this.validationResult = null;
    this.errorMessage = null;
    this.currentStep = 1;
    this.searchTerm = '';
    this.currentPage = 1;
    this.selectedErrorRow = null;
  }

  isDownloadingTemplate = false;

  downloadTemplate() {
    this.isDownloadingTemplate = true;
    this.errorMessage = null;

    this.svc.downloadPlantilla(this.templateType).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `plantilla_${this.templateType.toLowerCase()}.xlsx`;
        a.click();
        URL.revokeObjectURL(url);
        this.isDownloadingTemplate = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isDownloadingTemplate = false;
        if (err.status === 404) {
          this.errorMessage = `No hay ninguna plantilla ${this.templateType} subida aún. Contacte a SERNAPESCA.`;
        } else {
          this.errorMessage = 'Error al descargar la plantilla. Intente nuevamente.';
        }
        this.cdr.detectChanges();
      },
    });
  }

  setPage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  onSearch(term: string) {
    this.searchTerm = term;
    this.currentPage = 1;
  }

  selectRow(row: RowResult) {
    if (!row.isValid) {
      this.selectedErrorRow = this.selectedErrorRow?.rowNumber === row.rowNumber ? null : row;
      this.cdr.detectChanges();
    }
  }

  getErrorSummary(row: RowResult): string {
    return row.errors.map((e) => `Col ${e.column}: ${e.message}`).join('\n');
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
