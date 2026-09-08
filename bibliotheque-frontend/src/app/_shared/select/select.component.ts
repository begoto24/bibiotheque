import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SelectOption {
  value: any;
  label: string;
}

/**
 * Menu déroulant maison : remplace le <select> natif partout où on en a
 * besoin (Statut, Rôle, Livre, Adhérent...). Raison d'être unique : la
 * liste d'options d'un <select> natif est un widget du navigateur/OS que
 * le CSS ne peut pas recolorer de façon fiable — elle garde le bleu par
 * défaut quel que soit le thème de la page.
 *
 * Implémente ControlValueAccessor : s'utilise avec [(ngModel)] ou
 * formControlName exactement comme un <select>, sans code spécifique côté
 * formulaire appelant.
 */
@Component({
  selector: 'app-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './select.component.html',
  styleUrls: ['./select.component.css'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SelectComponent),
    multi: true
  }]
})
export class SelectComponent implements ControlValueAccessor {
  @Input() options: SelectOption[] = [];
  @Input() placeholder = '-- Sélectionner --';
  @Input() invalid = false;
  @Input() id?: string;

  isOpen = false;
  disabled = false;
  value: any = null;
  /** Ouvre vers le haut quand la place manque en dessous (ex. dernier champ
   *  d'une modale) — sinon la liste se retrouve coupée par le bord de la
   *  modale (overflow:hidden/auto sur ses conteneurs). */
  openUpward = false;

  private onChange: (value: any) => void = () => { };
  private onTouched: () => void = () => { };

  constructor(private elementRef: ElementRef<HTMLElement>) { }

  get selectedLabel(): string {
    const match = this.options.find(option => option.value === this.value);
    return match ? match.label : this.placeholder;
  }

  get hasValue(): boolean {
    return this.value !== null && this.value !== undefined && this.value !== '';
  }

  toggle(): void {
    if (this.disabled) {
      return;
    }
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.updateOpenDirection();
    } else {
      this.onTouched();
    }
  }

  private updateOpenDirection(): void {
    const rect = this.elementRef.nativeElement.getBoundingClientRect();
    const estimatedMenuHeight = Math.min(260, this.options.length * 40 + 12);
    const spaceBelow = window.innerHeight - rect.bottom;
    this.openUpward = spaceBelow < estimatedMenuHeight && rect.top > spaceBelow;
  }

  close(): void {
    if (this.isOpen) {
      this.isOpen = false;
      this.onTouched();
    }
  }

  selectOption(option: SelectOption): void {
    this.value = option.value;
    this.onChange(this.value);
    this.close();
  }

  isSelected(option: SelectOption): boolean {
    return option.value === this.value;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  writeValue(value: any): void {
    this.value = value;
  }

  registerOnChange(fn: (value: any) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
