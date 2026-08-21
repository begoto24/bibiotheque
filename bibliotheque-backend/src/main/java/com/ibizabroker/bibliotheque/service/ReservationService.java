package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatut;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BadRequestException;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final List<ReservationStatut> STATUTS_ACTIFS =
            Arrays.asList(ReservationStatut.EN_ATTENTE, ReservationStatut.DISPONIBLE);

    private static final List<ReservationStatut> STATUTS_TERMINAUX =
            Arrays.asList(ReservationStatut.ANNULEE, ReservationStatut.EXPIREE, ReservationStatut.HONOREE);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    public ReservationResponseDTO creer(ReservationRequestDTO request) {
        if (request.getLivreId() == null) {
            throw new BadRequestException("livreId manquant");
        }
        if (request.getAdherentId() == null) {
            throw new BadRequestException("adherentId manquant");
        }

        Books livre = booksRepository.findById(request.getLivreId())
                .orElseThrow(() -> new NotFoundException(
                        "Livre avec id " + request.getLivreId() + " introuvable."));
        Users adherent = usersRepository.findById(request.getAdherentId())
                .orElseThrow(() -> new NotFoundException(
                        "Adhérent avec id " + request.getAdherentId() + " introuvable."));

        // RG-01 : on ne peut réserver qu'un livre indisponible
        if (livre.getNoOfCopies() != null && livre.getNoOfCopies() > 0) {
            throw new ConflictException(
                    "RG-01 : on ne peut réserver qu'un livre indisponible.");
        }

        // RG-02 : une seule réservation active sur un même livre
        if (reservationRepository.existsByAdherent_UserIdAndLivre_BookIdAndStatutIn(
                adherent.getUserId(), livre.getBookId(), STATUTS_ACTIFS)) {
            throw new ConflictException(
                    "RG-02 : un adhérent ne peut avoir qu'une seule réservation active sur un même livre.");
        }

        // RG-03 : max 3 réservations actives
        long actives = reservationRepository.countByAdherent_UserIdAndStatutIn(
                adherent.getUserId(), STATUTS_ACTIFS);
        if (actives >= 3) {
            throw new ConflictException(
                    "RG-03 : un adhérent ne peut pas dépasser 3 réservations actives simultanées.");
        }

        LocalDateTime maintenant = LocalDateTime.now();
        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(maintenant);
        // RG-04
        reservation.setDateExpiration(maintenant.plusDays(7));
        reservation.setStatut(ReservationStatut.EN_ATTENTE);

        return toDto(reservationRepository.save(reservation));
    }

    public List<ReservationResponseDTO> lister(ReservationStatut statut, Integer adherentId) {
        List<Reservation> reservations;
        if (statut != null && adherentId != null) {
            reservations = reservationRepository.findByStatutAndAdherent_UserId(statut, adherentId);
        } else if (statut != null) {
            reservations = reservationRepository.findByStatut(statut);
        } else if (adherentId != null) {
            reservations = reservationRepository.findByAdherent_UserId(adherentId);
        } else {
            reservations = reservationRepository.findAll();
        }
        return reservations.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ReservationResponseDTO consulter(Integer id) {
        return toDto(trouverOu404(id));
    }

    public ReservationResponseDTO annuler(Integer id) {
        Reservation reservation = trouverOu404(id);

        // RG-06 : états terminaux non modifiables
        if (STATUTS_TERMINAUX.contains(reservation.getStatut())) {
            throw new ConflictException(
                    "RG-06 : une réservation " + reservation.getStatut()
                            + " ne peut plus changer d'état.");
        }

        // RG-05 : annulation seulement si EN_ATTENTE ou DISPONIBLE
        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new ConflictException(
                    "RG-05 : une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE.");
        }

        reservation.setStatut(ReservationStatut.ANNULEE);
        return toDto(reservationRepository.save(reservation));
    }

    public void supprimer(Integer id) {
        Reservation reservation = trouverOu404(id);
        reservationRepository.delete(reservation);
    }

    private Reservation trouverOu404(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Réservation avec id " + id + " introuvable."));
    }

    private ReservationResponseDTO toDto(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .livreId(reservation.getLivre().getBookId())
                .adherentId(reservation.getAdherent().getUserId())
                .dateReservation(reservation.getDateReservation())
                .dateExpiration(reservation.getDateExpiration())
                .statut(reservation.getStatut())
                .build();
    }
}
