import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Wrapper visuel label + champ + message d'erreur, utilisé autour d'un
 * <input>/<select> natif projeté via <ng-content>. Volontairement PAS un
 * ControlValueAccessor : l'input projeté garde son [(ngModel)]/required/
 * #ref="ngModel" tel quel dans le template parent. Ce composant ne fait que
 * mutualiser l'habillage (déjà dupliqué à l'identique dans tous les
 * formulaires) — zéro risque de casser un binding existant.
 */
@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './form-field.component.html',
  styleUrls: ['./form-field.component.css']
})
export class FormFieldComponent {
  @Input() label = '';
  @Input() icon?: string;
  @Input() for?: string;
  @Input() invalid: boolean | null = false;
  @Input() error = 'Ce champ est requis.';
}
