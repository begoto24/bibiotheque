import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Pastille de statut/catégorie (disponibilité, rôle, genre...). Purement
 * présentationnel : le variant choisit la teinte via les classes
 * .badge-{variant} déjà définies dans styles.css (pastel, dérivées de la
 * palette). Ajouter une nouvelle teinte = ajouter une classe .badge-xxx
 * dans styles.css, pas modifier ce composant.
 */
@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [ngClass]="'badge-' + variant">
      <i *ngIf="icon" class="ph" [ngClass]="icon" aria-hidden="true"></i>
      <ng-content></ng-content>
    </span>
  `
})
export class BadgeComponent {
  /** Doit correspondre à une classe .badge-{variant} existante dans styles.css
   *  (available, overdue, pending, cancelled, genre, role-admin, role-user,
   *  success, warning, danger, info, secondary...). */
  @Input() variant = 'secondary';
  @Input() icon?: string;
}
