import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ReservationStatus } from '../_model/reservation-status.enum';
import { UserAuthService } from '../_service/user-auth.service';
import { BooksService } from '../books/services/books.service';
import { BorrowService } from '../borrow/services/borrow.service';
import { ReservationService } from '../reservation/services/reservation.service';
import { UsersService } from '../users/services/users.service';

interface DashboardStats {
  books: number;
  users: number;
  pendingReservations: number;
  activeBorrows: number;
}

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  loadingStats = false;
  statsError = '';
  stats: DashboardStats = { books: 0, users: 0, pendingReservations: 0, activeBorrows: 0 };

  constructor(
    private userAuthService: UserAuthService,
    private booksService: BooksService,
    private usersService: UsersService,
    private reservationService: ReservationService,
    private borrowService: BorrowService
  ) { }

  ngOnInit(): void {
    if (this.isAdmin()) {
      this.loadStats();
    }
  }

  isLoggedIn(): boolean {
    return !!this.userAuthService.isLoggedIn();
  }

  isAdmin(): boolean {
    const roles: any[] = this.userAuthService.getRoles() || [];
    return roles.some(role => role?.roleName === 'Admin' || role === 'Admin');
  }

  loadStats(): void {
    this.loadingStats = true;
    this.statsError = '';
    forkJoin({
      books: this.booksService.getBooks(),
      users: this.usersService.getUsers(),
      reservations: this.reservationService.getReservations({ status: ReservationStatus.EN_ATTENTE }),
      borrows: this.borrowService.getBorrows()
    }).subscribe({
      next: ({ books, users, reservations, borrows }) => {
        this.stats = {
          books: books.length,
          users: users.length,
          pendingReservations: reservations.length,
          activeBorrows: borrows.filter(borrow => !borrow.returnDate).length
        };
        this.loadingStats = false;
      },
      error: error => {
        this.statsError = error.message;
        this.loadingStats = false;
      }
    });
  }
}
