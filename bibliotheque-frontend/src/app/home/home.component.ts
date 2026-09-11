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

interface AdherentStats {
  activeReservations: number;
  activeBorrows: number;
  overdueBorrows: number;
}

const MAX_ACTIVE_RESERVATIONS = 3; // RG-03, cf. séance 2

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  loadingStats = false;
  statsError = '';
  stats: DashboardStats = { books: 0, users: 0, pendingReservations: 0, activeBorrows: 0 };

  loadingAdherentStats = false;
  adherentStatsError = '';
  adherentStats: AdherentStats = { activeReservations: 0, activeBorrows: 0, overdueBorrows: 0 };
  readonly maxActiveReservations = MAX_ACTIVE_RESERVATIONS;

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
    } else if (this.isLoggedIn()) {
      this.loadAdherentStats();
    }
  }

  isLoggedIn(): boolean {
    return !!this.userAuthService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.userAuthService.isAdmin();
  }

  getUserName(): string {
    return this.userAuthService.getName() || 'Adhérent';
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

  /**
   * Tableau de bord d'un ADHERENT : uniquement ses propres données.
   * getReservations() est déjà filtré côté serveur sur l'appelant (RS-05),
   * pas besoin de passer son id ici. getBorrowsByUser en revanche l'exige.
   */
  loadAdherentStats(): void {
    this.loadingAdherentStats = true;
    this.adherentStatsError = '';
    const userId = this.userAuthService.getUserId();
    forkJoin({
      reservations: this.reservationService.getReservations(),
      borrows: this.borrowService.getBorrowsByUser(userId)
    }).subscribe({
      next: ({ reservations, borrows }) => {
        const now = new Date();
        const activeBorrows = borrows.filter(borrow => !borrow.returnDate);
        this.adherentStats = {
          activeReservations: reservations.filter(reservation => reservation.active).length,
          activeBorrows: activeBorrows.length,
          overdueBorrows: activeBorrows.filter(borrow => new Date(borrow.dueDate) < now).length
        };
        this.loadingAdherentStats = false;
      },
      error: error => {
        this.adherentStatsError = error.message;
        this.loadingAdherentStats = false;
      }
    });
  }
}
