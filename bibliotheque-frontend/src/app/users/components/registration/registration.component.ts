import { Component, ElementRef, EventEmitter, Output } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Users } from '../../../_model/users';
import { SelectOption } from '../../../_shared/select/select.component';
import { UsersService } from '../../services/users.service';

/**
 * Bouton "+ Ajouter un utilisateur" + modale de création, embarqué dans la
 * liste des utilisateurs (même pattern que <app-reservation-form>). Émet
 * (userCreated) pour que la liste parente se rafraîchisse.
 */
@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
})
export class RegistrationComponent {
  @Output() userCreated = new EventEmitter<void>();

  user: Users = new Users();
  submitting = false;
  error = '';
  successMessage = '';
  isModalOpen = false;
  readonly roleOptions: SelectOption[] = [
    { value: 'Admin', label: 'Admin' },
    { value: 'User', label: 'User' }
  ];

  private autoCloseTimeout?: ReturnType<typeof setTimeout>;

  constructor(private usersService: UsersService, private elementRef: ElementRef) { }

  openModal(): void {
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.error = '';
    this.successMessage = '';
    this.user = new Users();
    clearTimeout(this.autoCloseTimeout);
  }

  onSubmit(form: NgForm): void {
    if (form.invalid) {
      form.form.markAllAsTouched();
      this.focusFirstInvalidField();
      return;
    }
    this.saveUser();
  }

  private saveUser(): void {
    this.submitting = true;
    this.error = '';
    this.successMessage = '';
    this.usersService.createUser(this.user).subscribe({
      next: () => {
        this.submitting = false;
        this.userCreated.emit();
        this.successMessage = 'Utilisateur créé avec succès.';
        this.autoCloseTimeout = setTimeout(() => this.closeModal(), 1200);
      },
      error: error => {
        this.submitting = false;
        this.error = error.message;
      }
    });
  }

  private focusFirstInvalidField(): void {
    setTimeout(() => {
      const invalidField: HTMLElement | null = this.elementRef.nativeElement.querySelector('.is-invalid');
      invalidField?.focus();
      invalidField?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }
}
