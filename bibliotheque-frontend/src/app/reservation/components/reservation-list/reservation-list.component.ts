import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Reservation } from '../../../_model/reservation.model';
import { ReservationStatus, ReservationStatusColors, ReservationStatusLabels } from '../../../_model/reservation-status.enum';

@Component({
  selector: 'app-reservation-list',
  templateUrl: './reservation-list.component.html',
  styleUrls: ['./reservation-list.component.css']
})
export class ReservationListComponent {
  @Input() reservations: Reservation[] = [];
  @Input() loading = false;
  @Input() error = '';

  @Output() retry = new EventEmitter<void>();
  @Output() reservationCancelled = new EventEmitter<number>();

  reservationPendingCancellation: Reservation | null = null;

  getStatusLabel(status: ReservationStatus): string {
    return ReservationStatusLabels[status] || status;
  }

  getStatusColor(status: ReservationStatus): string {
    return ReservationStatusColors[status] || 'secondary';
  }

  canCancel(reservation: Reservation): boolean {
    return reservation.status === ReservationStatus.EN_ATTENTE || reservation.status === ReservationStatus.DISPONIBLE;
  }

  askCancelReservation(reservation: Reservation): void {
    this.reservationPendingCancellation = reservation;
  }

  dismissCancelReservation(): void {
    this.reservationPendingCancellation = null;
  }

  confirmCancelReservation(): void {
    if (!this.reservationPendingCancellation) {
      return;
    }
    this.reservationCancelled.emit(this.reservationPendingCancellation.id);
    this.reservationPendingCancellation = null;
  }

  formatDate(value: string | Date): string {
    return value ? new Date(value).toLocaleDateString('fr-FR') : '';
  }
}
