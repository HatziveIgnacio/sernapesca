import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  imports: [RouterLink],
  templateUrl: './placeholder.html',
  styleUrl: './placeholder.css',
})
export class Placeholder {
  private readonly route = inject(ActivatedRoute);

  readonly title = this.route.snapshot.data['title'] ?? 'Sección';
  readonly description = this.route.snapshot.data['description'] ?? 'Esta sección está pendiente de implementación.';
}
