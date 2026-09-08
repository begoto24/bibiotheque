import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { SearchService } from '../../../_core/services/search.service';
import { Books } from '../../../_model/books'
import { BooksService } from '../../services/books.service';
import { CreateBookComponent } from '../create-book/create-book.component';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css']
})
export class BooksListComponent implements OnInit, OnDestroy {

  @ViewChild(CreateBookComponent) createBookForm!: CreateBookComponent;

  books: Books[] = [];
  filteredBooks: Books[] = [];
  loading = false;
  error = '';
  bookPendingDeletion: Books | null = null;

  private searchQuery = '';
  private searchSubscription?: Subscription;

  constructor(private booksService: BooksService,
    private router: Router,
    private searchService: SearchService) { }

  ngOnInit(): void {
    this.getBooks();
    this.searchSubscription = this.searchService.query$.subscribe(query => {
      this.searchQuery = query;
      this.applyFilter();
    });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  getBooks() {
    this.loading = true;
    this.error = '';
    this.booksService.getBooks().pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: data => {
        this.books = data;
        this.applyFilter();
      },
      error: error => this.error = error.message
    });
  }

  private applyFilter(): void {
    if (!this.searchQuery) {
      this.filteredBooks = this.books;
      return;
    }
    this.filteredBooks = this.books.filter(book =>
      book.bookName?.toLowerCase().includes(this.searchQuery) ||
      book.bookAuthor?.toLowerCase().includes(this.searchQuery) ||
      book.bookGenre?.toLowerCase().includes(this.searchQuery)
    );
  }

  updateBook(bookId: number) {
    this.router.navigate(['/books/update', bookId ]);
  }

  askDeleteBook(book: Books) {
    this.bookPendingDeletion = book;
  }

  cancelDeleteBook() {
    this.bookPendingDeletion = null;
  }

  confirmDeleteBook() {
    if (!this.bookPendingDeletion) {
      return;
    }
    this.deleteBook(this.bookPendingDeletion.bookId);
  }

  deleteBook(bookId: number) {
    this.booksService.deleteBook(bookId).subscribe({
      next: () => {
        this.bookPendingDeletion = null;
        this.getBooks();
      },
      error: error => {
        this.bookPendingDeletion = null;
        this.error = error.message;
      }
    });
  }

  bookDetails(bookId: number) {
    this.router.navigate(['/books/details', bookId ]);
  }

}
