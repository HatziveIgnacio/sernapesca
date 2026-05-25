import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PlantillaMetadata {
  id: string;
  anio: number;
  mes: number;
  tipo: 'RRA' | 'RRA_FAR';
  nombreArchivo: string;
  tamanoBytes: number;
  fechaSubida: string;
}

@Injectable({ providedIn: 'root' })
export class PlantillasService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.javaApi}/plantillas`;

  /** Sube la plantilla Excel mensual al backend Java */
  upload(file: File, anio: number, mes: number, tipo: string): Observable<PlantillaMetadata> {
    const form = new FormData();
    form.append('file', file);
    form.append('anio', String(anio));
    form.append('mes', String(mes));
    form.append('tipo', tipo);
    return this.http.post<PlantillaMetadata>(`${this.api}/upload`, form);
  }

  /** Lista todas las plantillas registradas */
  listar(): Observable<PlantillaMetadata[]> {
    return this.http.get<PlantillaMetadata[]>(this.api);
  }

  /** URL de descarga de una plantilla específica */
  getDownloadUrl(id: string): string {
    return `${this.api}/${id}/download`;
  }
}
