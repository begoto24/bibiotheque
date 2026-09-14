import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { formatBorrowDate } from '../../../_core/utils/date.util';
import { Books } from '../../../_model/books';
import { Borrow } from '../../../_model/borrow';
import { BooksService } from '../../../books/services/books.service';
import { BorrowService } from '../../services/borrow.service';
import { UserAuthService } from '../../../_service/user-auth.service';

@Component({
  selector: 'app-borrow-book',
  templateUrl: './borrow-book.component.html',
  styleUrls: ['./borrow-book.component.css']
})
export class BorrowBookComponent implements OnInit {

  books: Books[] = [];
  loading = false;
  error = '';
  success = '';

  /** Livre en attente de confirmation (dialogue avant l'emprunt effectif). */
  pendingBorrow: Books | null = null;

  constructor(
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private borrowService: BorrowService,
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
  }

  private getBooks() {
    this.booksService.getBooks().subscribe({
      next: data => this.books = data,
      error: error => this.error = error.message
    });
  }

  borrow: Borrow = new Borrow();

  askBorrow(book: Books): void {
    this.pendingBorrow = book;
  }

  dismissBorrow(): void {
    this.pendingBorrow = null;
  }

  confirmBorrow(): void {
    if (!this.pendingBorrow) {
      return;
    }
    const bookId = this.pendingBorrow.bookId;
    this.pendingBorrow = null;
    this.loading = true;
    this.error = '';
    this.success = '';
    this.borrowService.borrowBook(bookId, this.userId).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      // Le backend renvoie l'emprunt créé (avec dueDate calculée serveur,
      // +7 jours — RG séance 1) : on l'affiche pour que l'adhérent sache
      // tout de suite jusqu'à quand il a le livre, sans devoir aller sur
      // la page "Rendre" pour le découvrir.
      next: created => {
        this.success = `Livre emprunté avec succès. À rendre avant le ${formatBorrowDate(created.dueDate)}.`;
        this.getBooks();
      },
      error: error => this.error = error.message
    });
  }
}
