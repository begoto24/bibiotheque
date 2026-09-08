import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

/**
 * Boîte de dialogue de confirmation générique (suppression, annulation...).
 * Composant purement présentationnel : il n'appelle aucun service et ne
 * connaît rien de la logique métier de l'écran qui l'utilise. L'écran
 * hôte décide quoi faire via les événements (confirmed) / (cancelled).
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.css']
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Confirmer';
  @Input() message = 'Êtes-vous sûr de vouloir continuer ?';
  @Input() confirmLabel = 'Confirmer';
  @Input() cancelLabel = 'Annuler';
  @Input() variant: 'danger' | 'default' = 'default';

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onConfirm(): void {
    this.confirmed.emit();
  }

  onCancel(): void {
    this.cancelled.emit();
  }

  onBackdropClick(): void {
    this.onCancel();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) {
      this.onCancel();
    }
  }
}
