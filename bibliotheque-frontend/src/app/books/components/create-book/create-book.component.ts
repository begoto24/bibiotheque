import { Component, ElementRef, EventEmitter, Output } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Books } from '../../../_model/books';
import { BooksService } from '../../services/books.service';

/**
 * Bouton "+ Ajouter un livre" + modale de création, embarqué directement
 * dans la liste des livres (même pattern que <app-reservation-form>). Émet
 * (bookCreated) pour que la liste parente se rafraîchisse.
 */
@Component({
  selector: 'app-create-book',
  templateUrl: './create-book.component.html',
  styleUrls: ['./create-book.component.css']
})
export class CreateBookComponent {
  @Output() bookCreated = new EventEmitter<void>();

  book: Books = new Books();
  submitting = false;
  error = '';
  isModalOpen = false;

  private autoCloseTimeout?: ReturnType<typeof setTimeout>;

  constructor(private booksService: BooksService, private elementRef: ElementRef) { }

  openModal(): void {
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.error = '';
    this.book = new Books();
    clearTimeout(this.autoCloseTimeout);
  }

  onSubmit(form: NgForm): void {
    if (form.invalid) {
      form.form.markAllAsTouched();
      this.focusFirstInvalidField();
      return;
    }
    this.saveBook();
  }

  private saveBook(): void {
    this.submitting = true;
    this.error = '';
    this.booksService.createBook(this.book).subscribe({
      next: () => {
        this.submitting = false;
        this.bookCreated.emit();
        this.autoCloseTimeout = setTimeout(() => this.closeModal(), 600);
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
