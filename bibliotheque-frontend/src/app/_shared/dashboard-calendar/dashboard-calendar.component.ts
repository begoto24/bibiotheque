import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { BadgeComponent } from '../badge/badge.component';
import { ButtonComponent } from '../button/button.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { SkeletonComponent } from '../skeleton/skeleton.component';
import { parseBorrowDate } from '../../_core/utils/date.util';
import { Books } from '../../_model/books';
import { Borrow } from '../../_model/borrow';
import { Reservation } from '../../_model/reservation.model';
import { ReservationStatus } from '../../_model/reservation-status.enum';
import { Users } from '../../_model/users';
import { UserAuthService } from '../../_service/user-auth.service';
import { BooksService } from '../../books/services/books.service';
import { BorrowService } from '../../borrow/services/borrow.service';
import { ReservationService } from '../../reservation/services/reservation.service';
import { UsersService } from '../../users/services/users.service';

type DayEventType = 'reservation-created' | 'reservation-expires' | 'borrow-issued' | 'borrow-due';

interface DayEvent {
  type: DayEventType;
  icon: string;
  label: string;
  bookTitle: string;
  personName: string | null;
  variant: string;
  reservation?: Reservation;
  borrow?: Borrow;
  canAct: boolean;
}

interface CalendarDay {
  date: Date;
  dateKey: string;
  dayNumber: number;
  inCurrentMonth: boolean;
  isToday: boolean;
  events: DayEvent[];
}

const WEEKDAY_LABELS = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
const MONTH_LABELS = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
];

/**
 * Calendrier interactif du tableau de bord : vue mensuelle des réservations
 * et emprunts, navigation entre mois, sélection d'un jour pour voir/agir sur
 * ses événements (annuler une réservation, marquer un emprunt comme rendu).
 *
 * Volontairement PAS un calendrier de prise de rendez-vous : les dates de
 * réservation/emprunt sont toujours calculées côté serveur (RG-04, RS-04 —
 * cf. séances 2 et 4), donc aucune création avec date choisie librement ici.
 * L'interactivité porte sur la consultation et l'action sur l'existant.
 */
@Component({
  selector: 'app-dashboard-calendar',
  standalone: true,
  imports: [CommonModule, RouterLink, ButtonComponent, BadgeComponent, SkeletonComponent, ConfirmDialogComponent],
  templateUrl: './dashboard-calendar.component.html',
  styleUrls: ['./dashboard-calendar.component.css']
})
export class DashboardCalendarComponent implements OnInit {
  readonly weekdayLabels = WEEKDAY_LABELS;

  loading = false;
  error = '';
  actionError = '';
  actionSuccess = '';
  actionLoading = false;

  visibleMonth: Date;
  days: CalendarDay[] = [];
  selectedDateKey: string;

  pendingCancel: Reservation | null = null;
  pendingReturn: Borrow | null = null;

  private eventsByDate = new Map<string, DayEvent[]>();
  private readonly todayKey: string;

  constructor(
    private userAuthService: UserAuthService,
    private reservationService: ReservationService,
    private borrowService: BorrowService,
    private booksService: BooksService,
    private usersService: UsersService
  ) {
    const now = new Date();
    this.visibleMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    this.todayKey = this.toDateKey(now)!;
    this.selectedDateKey = this.todayKey;
  }

  ngOnInit(): void {
    this.load();
  }

  get monthLabel(): string {
    return `${MONTH_LABELS[this.visibleMonth.getMonth()]} ${this.visibleMonth.getFullYear()}`;
  }

  get selectedDay(): CalendarDay | undefined {
    return this.days.find(day => day.dateKey === this.selectedDateKey);
  }

  get selectedDateLabel(): string {
    const day = this.selectedDay;
    return day ? day.date.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }) : '';
  }

  get isSelectedToday(): boolean {
    return this.selectedDateKey === this.todayKey;
  }

  get isAdherentView(): boolean {
    return !this.userAuthService.isAdmin();
  }

  previousMonth(): void {
    this.visibleMonth = new Date(this.visibleMonth.getFullYear(), this.visibleMonth.getMonth() - 1, 1);
    this.rebuildGrid();
  }

  nextMonth(): void {
    this.visibleMonth = new Date(this.visibleMonth.getFullYear(), this.visibleMonth.getMonth() + 1, 1);
    this.rebuildGrid();
  }

  goToToday(): void {
    const now = new Date();
    this.visibleMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    this.selectedDateKey = this.todayKey;
    this.rebuildGrid();
  }

  selectDay(day: CalendarDay): void {
    this.selectedDateKey = day.dateKey;
    this.actionError = '';
    this.actionSuccess = '';
  }

  askCancelReservation(event: DayEvent): void {
    this.pendingCancel = event.reservation || null;
  }

  dismissCancelReservation(): void {
    this.pendingCancel = null;
  }

  confirmCancelReservation(): void {
    if (!this.pendingCancel) {
      return;
    }
    this.actionLoading = true;
    this.actionError = '';
    this.reservationService.cancelReservation(this.pendingCancel.id).subscribe({
      next: () => {
        this.actionLoading = false;
        this.actionSuccess = 'Réservation annulée.';
        this.pendingCancel = null;
        this.load();
      },
      error: error => {
        this.actionLoading = false;
        this.actionError = error.message;
        this.pendingCancel = null;
      }
    });
  }

  askMarkReturned(event: DayEvent): void {
    this.pendingReturn = event.borrow || null;
  }

  dismissMarkReturned(): void {
    this.pendingReturn = null;
  }

  confirmMarkReturned(): void {
    if (!this.pendingReturn) {
      return;
    }
    this.actionLoading = true;
    this.actionError = '';
    const payload = new Borrow();
    payload.borrowId = this.pendingReturn.borrowId;
    this.borrowService.returnBook(payload).subscribe({
      next: () => {
        this.actionLoading = false;
        this.actionSuccess = 'Livre marqué comme rendu.';
        this.pendingReturn = null;
        this.load();
      },
      error: error => {
        this.actionLoading = false;
        this.actionError = error.message;
        this.pendingReturn = null;
      }
    });
  }

  private load(): void {
    this.loading = true;
    this.error = '';
    const isAdmin = this.userAuthService.isAdmin();
    const myUserId = this.userAuthService.getUserId();

    forkJoin({
      reservations: this.reservationService.getReservations(),
      borrows: isAdmin ? this.borrowService.getBorrows() : this.borrowService.getBorrowsByUser(myUserId),
      books: this.booksService.getBooks(),
      users: isAdmin ? this.usersService.getUsers() : of([] as Users[])
    }).subscribe({
      next: ({ reservations, borrows, books, users }) => {
        this.buildIndex(reservations, borrows, books, users, isAdmin);
        this.rebuildGrid();
        this.loading = false;
      },
      error: error => {
        this.error = error.message;
        this.loading = false;
      }
    });
  }

  private buildIndex(reservations: Reservation[], borrows: Borrow[], books: Books[], users: Users[], isAdmin: boolean): void {
    const bookTitleById = new Map(books.map(book => [book.bookId, book.bookName]));
    const userNameById = new Map(users.map(user => [user.userId, user.name]));
    const map = new Map<string, DayEvent[]>();

    for (const reservation of reservations) {
      this.indexReservation(reservation, map);
    }
    for (const borrow of borrows) {
      this.indexBorrow(borrow, bookTitleById, isAdmin ? userNameById : null, map);
    }

    this.eventsByDate = map;
  }

  private indexReservation(reservation: Reservation, map: Map<string, DayEvent[]>): void {
    const active = reservation.status === ReservationStatus.EN_ATTENTE || reservation.status === ReservationStatus.DISPONIBLE;

    const createdKey = this.toDateKey(reservation.reservationDate);
    if (createdKey) {
      this.pushEvent(map, createdKey, {
        type: 'reservation-created',
        icon: 'ph-calendar-plus',
        label: 'Réservation créée',
        bookTitle: reservation.bookTitle,
        personName: reservation.userName,
        variant: 'info',
        reservation,
        canAct: active
      });
    }

    // Une réservation honorée/annulée n'a plus d'échéance à surveiller.
    const expirationKey = active ? this.toDateKey(reservation.expirationDate) : null;
    if (expirationKey) {
      this.pushEvent(map, expirationKey, {
        type: 'reservation-expires',
        icon: 'ph-hourglass',
        label: 'Réservation à expirer',
        bookTitle: reservation.bookTitle,
        personName: reservation.userName,
        variant: 'warning',
        reservation,
        canAct: active
      });
    }
  }

  private indexBorrow(borrow: Borrow, bookTitleById: Map<number, string>, userNameById: Map<number, string> | null, map: Map<string, DayEvent[]>): void {
    const bookTitle = bookTitleById.get(borrow.bookId) || `Livre #${borrow.bookId}`;
    const personName = userNameById ? (userNameById.get(borrow.userId) || null) : null;

    const issuedKey = this.toDateKey(borrow.issueDate);
    if (issuedKey) {
      this.pushEvent(map, issuedKey, {
        type: 'borrow-issued',
        icon: 'ph-hand-heart',
        label: 'Emprunté',
        bookTitle,
        personName,
        variant: 'success',
        borrow,
        canAct: false
      });
    }

    if (!borrow.returnDate) {
      const dueKey = this.toDateKey(borrow.dueDate);
      if (dueKey) {
        const overdue = (parseBorrowDate(borrow.dueDate) || new Date(0)) < new Date();
        this.pushEvent(map, dueKey, {
          type: 'borrow-due',
          icon: overdue ? 'ph-warning' : 'ph-clock',
          label: overdue ? 'Retour en retard' : 'Retour attendu',
          bookTitle,
          personName,
          variant: overdue ? 'danger' : 'warning',
          borrow,
          canAct: true
        });
      }
    }
  }

  private pushEvent(map: Map<string, DayEvent[]>, key: string, event: DayEvent): void {
    const existing = map.get(key);
    if (existing) {
      existing.push(event);
    } else {
      map.set(key, [event]);
    }
  }

  private rebuildGrid(): void {
    const year = this.visibleMonth.getFullYear();
    const month = this.visibleMonth.getMonth();
    const firstOfMonth = new Date(year, month, 1);
    // getDay() : 0 = dimanche ... 6 = samedi -> décalage pour une semaine commençant le lundi.
    const firstWeekdayOffset = (firstOfMonth.getDay() + 6) % 7;
    const gridStart = new Date(year, month, 1 - firstWeekdayOffset);

    const days: CalendarDay[] = [];
    for (let i = 0; i < 42; i++) {
      const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + i);
      const key = this.toDateKey(date)!;
      days.push({
        date,
        dateKey: key,
        dayNumber: date.getDate(),
        inCurrentMonth: date.getMonth() === month,
        isToday: key === this.todayKey,
        events: this.eventsByDate.get(key) || []
      });
    }
    this.days = days;
  }

  private toDateKey(value: string | Date | null | undefined): string | null {
    // parseBorrowDate gère aussi bien le "dd-MM-yyyy" propre à Borrow que
    // l'ISO utilisé partout ailleurs (ex. Reservation) — un seul point de
    // parsing pour les deux formats de date que ce calendrier combine.
    const date = parseBorrowDate(value);
    if (!date) {
      return null;
    }
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
