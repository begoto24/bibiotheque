import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonComponent } from '../button/button.component';
import { SkeletonComponent } from '../skeleton/skeleton.component';

/**
 * Enrobe une liste (tableau) avec ses 3 états standards — chargement / erreur
 * / vide —, jusqu'ici copiés-collés à l'identique dans chaque composant de
 * liste. Le <table> réel reste dans le template parent (projeté), ce
 * composant ne connaît ni ses colonnes ni ses données : uniquement l'état.
 */
@Component({
  selector: 'app-table-state',
  standalone: true,
  imports: [CommonModule, ButtonComponent, SkeletonComponent],
  templateUrl: './table-state.component.html'
})
export class TableStateComponent {
  @Input() loading = false;
  @Input() error: string | null = '';
  @Input() empty = false;
  @Input() loadingLabel = 'Chargement...';
  @Input() emptyIcon = 'ph-tray';
  @Input() emptyLabel = 'Aucun élément trouvé.';
  /** Nombre de lignes fantômes affichées pendant [loading]. */
  @Input() skeletonRowCount = 5;

  @Output() retry = new EventEmitter<void>();

  get skeletonRows(): number[] {
    return Array(this.skeletonRowCount).fill(0);
  }
}
