import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Books } from '../../../_model/books';
import { BooksService } from '../../services/books.service';

@Component({
  selector: 'app-update-book',
  templateUrl: './update-book.component.html',
  styleUrls: ['./update-book.component.css']
})
export class UpdateBookComponent implements OnInit {

  bookId: number;
  book: Books = new Books();
  submitting = false;
  error = '';

  constructor(private booksService: BooksService,
    private route: ActivatedRoute,
    private router: Router) { }

  ngOnInit(): void {
    this.bookId = this.route.snapshot.params['bookId'];
    this.booksService.getBookById(this.bookId).subscribe({
      next: data => this.book = data,
      error: error => this.error = error.message
    });
  }

  onSubmit() {
    this.submitting = true;
    this.error = '';
    this.booksService.updateBook(this.bookId, this.book).subscribe({
      next: () => this.goToBooksList(),
      error: error => {
        this.submitting = false;
        this.error = error.message;
      }
    });
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

}
