import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Barre "fantôme" animée (shimmer), brique de base des états de chargement
 * façon Notion/Linear. Purement visuel : aucune donnée, aucune logique.
 */
@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `<span
    class="skeleton"
    [ngClass]="'skeleton--' + shape"
    [style.width]="width"
    [style.height]="height"
  ></span>`
})
export class SkeletonComponent {
  @Input() shape: 'text' | 'circle' | 'block' = 'text';
  @Input() width = '100%';
  @Input() height?: string;
}
