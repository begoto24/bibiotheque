import { ReservationStatus } from './reservation-status.enum';

export interface Reservation {
  id: number;
  bookId: number;
  bookTitle: string;
  userId: number;
  userName: string;
  reservationDate: string | Date;
  expirationDate: string | Date;
  status: ReservationStatus;
  active: boolean;
}

export interface ReservationRequest {
  bookId: number;
  // Optionnel : un ADHERENT réserve toujours pour lui-même (identité prise du
  // token côté serveur, séance 4 / RS-04) — seul un BIBLIOTHECAIRE le renseigne.
  adherentId?: number;
}

export interface ReservationFilters {
  status?: ReservationStatus | string;
  userId?: number;
}
