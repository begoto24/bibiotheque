import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterModule } from '@angular/router';

export type ButtonVariant = 'primary' | 'secondary' | 'success' | 'danger' | 'outline';
export type ButtonSize = 'default' | 'sm';

/**
 * Bouton standard de l'app : gabarit (hauteur/police), variantes de couleur
 * et icône cohérents partout où il est utilisé (formulaires, actions de
 * tableau, empty states...). Purement présentationnel : émet (clicked),
 * ne connaît rien de la logique métier appelante.
 *
 * Deux modes, selon la présence de [routerLink] :
 * - avec [routerLink] -> rendu en <a>, pour la navigation ;
 * - sans -> rendu en <button>, pour une action locale ((clicked)).
 */
@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './button.component.html',
  styleUrls: ['./button.component.css']
})
export class ButtonComponent {
  @Input() variant: ButtonVariant = 'primary';
  @Input() size: ButtonSize = 'default';
  @Input() icon?: string;
  @Input() type: 'button' | 'submit' = 'button';
  @Input() disabled = false;
  @Input() loading = false;
  @Input() loadingText = 'Chargement...';
  @Input() routerLink?: string | any[];

  @Output() clicked = new EventEmitter<void>();

  private static readonly VARIANT_CLASS: Record<ButtonVariant, string> = {
    primary: 'btn-primary',
    secondary: 'btn-info',
    success: 'btn-success',
    danger: 'btn-danger',
    outline: 'btn-outline'
  };

  get variantClass(): string {
    return ButtonComponent.VARIANT_CLASS[this.variant];
  }

  get sizeClass(): string {
    return this.size === 'sm' ? 'btn-sm' : '';
  }

  onClick(): void {
    if (this.disabled || this.loading) {
      return;
    }
    this.clicked.emit();
  }
}
