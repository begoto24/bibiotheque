import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { Books } from '../../../_model/books';
import { ReservationRequest } from '../../../_model/reservation.model';
import { Users } from '../../../_model/users';
import { SelectOption } from '../../../_shared/select/select.component';
import { BooksService } from '../../../books/services/books.service';
import { UsersService } from '../../../users/services/users.service';
import { UserAuthService } from '../../../_service/user-auth.service';
import { ReservationService } from '../../services/reservation.service';

@Component({
  selector: 'app-reservation-form',
  templateUrl: './reservation-form.component.html',
  styleUrls: ['./reservation-form.component.css']
})
export class ReservationFormComponent implements OnInit, OnDestroy {
  @Output() reservationCreated = new EventEmitter<void>();

  reservationForm: FormGroup;
  books: Books[] = [];
  users: Users[] = [];
  loading = false;
  serverError = '';
  successMessage = '';
  isModalOpen = false;

  /**
   * Un ADHERENT (rôle "User") réserve toujours pour lui-même — l'API l'impose
   * côté serveur (RS-04, l'identité vient du token, jamais du corps). Le champ
   * Adhérent n'a donc de sens que pour un BIBLIOTHECAIRE (rôle "Admin"), qui
   * choisit pour qui il réserve. En le masquant pour un simple adhérent, on
   * évite aussi un GET /admin/users qui lui renverrait 403 (réservé Admin).
   */
  readonly isBibliothecaire: boolean;

  private autoCloseTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private formBuilder: FormBuilder,
    private reservationService: ReservationService,
    private booksService: BooksService,
    private usersService: UsersService,
    userAuthService: UserAuthService
  ) {
    this.isBibliothecaire = userAuthService.isAdmin();
    this.reservationForm = this.formBuilder.group({
      bookId: ['', Validators.required],
      adherentId: ['', this.isBibliothecaire ? Validators.required : []]
    });
  }

  ngOnInit(): void {
    this.loadBooks();
    if (this.isBibliothecaire) {
      this.loadUsers();
    }
  }

  get bookOptions(): SelectOption[] {
    return this.books.map(book => ({ value: book.bookId, label: `${book.bookName} (${book.bookAuthor})` }));
  }

  get adherentOptions(): SelectOption[] {
    return this.users.map(user => ({ value: user.userId, label: `${user.name} (${user.username})` }));
  }

  ngOnDestroy(): void {
    clearTimeout(this.autoCloseTimeout);
  }

  openModal(): void {
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.serverError = '';
    this.successMessage = '';
    clearTimeout(this.autoCloseTimeout);
  }

  submit(): void {
    if (this.reservationForm.invalid) {
      this.reservationForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.serverError = '';
    this.successMessage = '';
    // adherentId omis pour un ADHERENT : envoyer '' (valeur initiale du champ
    // masqué) casserait la désérialisation JSON -> Integer côté serveur.
    const payload: ReservationRequest = { bookId: this.reservationForm.value.bookId };
    if (this.isBibliothecaire) {
      payload.adherentId = this.reservationForm.value.adherentId;
    }
    this.reservationService.createReservation(payload).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: () => {
        this.reservationForm.reset();
        this.successMessage = 'Réservation créée avec succès.';
        this.reservationCreated.emit();
        // Laisse le message de succès s'afficher un court instant avant de
        // refermer la modale automatiquement (évite un clic supplémentaire).
        this.autoCloseTimeout = setTimeout(() => this.closeModal(), 1200);
      },
      error: error => this.serverError = error.message
    });
  }

  private loadBooks(): void {
    this.booksService.getBooks().subscribe({
      next: books => this.books = books,
      error: error => this.serverError = error.message
    });
  }

  private loadUsers(): void {
    this.usersService.getUsers().subscribe({
      next: users => this.users = users,
      error: error => this.serverError = error.message
    });
  }
}
