package com.ibizabroker.bibliotheque.service.impl;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.IReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements IReservationService {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            Arrays.asList(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public ReservationResponse createReservation(ReservationRequest request, Integer callerId, boolean bibliothecaire) {
        if (request.getBookId() == null) {
            throw new IllegalArgumentException("bookId est obligatoire");
        }

        // RS-04 : l'identite du createur vient du token, jamais du corps de la requete.
        // Un ADHERENT ne peut reserver que pour lui-meme : on ignore volontairement
        // request.getAdherentId() et on force callerId, meme s'il a envoye autre chose.
        // Seul un BIBLIOTHECAIRE peut reserver "pour n'importe qui" via adherentId.
        Integer targetAdherentId = bibliothecaire ? request.getAdherentId() : callerId;
        if (bibliothecaire && targetAdherentId == null) {
            throw new IllegalArgumentException("adherentId est obligatoire");
        }

        Books book = booksRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Livre non trouvé avec l'id: " + request.getBookId()));

        Users user = usersRepository.findById(targetAdherentId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé avec l'id: " + targetAdherentId));

        // RG-01 : On ne peut réserver qu'un livre indisponible
        if (book.getNoOfCopies() > 0) {
            throw new ConflictException("RG-01: Impossible de réserver un livre disponible. Exemplaires disponibles: " + book.getNoOfCopies());
        }

        // RG-02 : Un adhérent ne peut avoir qu'une seule réservation active sur un même livre
        boolean hasActiveReservation = reservationRepository
                .existsByUserIdAndBookIdAndStatusIn(targetAdherentId, request.getBookId(), ACTIVE_STATUSES);
        if (hasActiveReservation) {
            throw new ConflictException("RG-02: Vous avez déjà une réservation active pour ce livre");
        }

        // RG-03 : Un adhérent ne peut pas dépasser 3 réservations actives simultanées
        long activeCount = reservationRepository.countByUserIdAndStatusIn(targetAdherentId, ACTIVE_STATUSES);
        if (activeCount >= 3) {
            throw new ConflictException("RG-03: Vous avez atteint le nombre maximum de réservations actives (3)");
        }

        // Créer la réservation (RG-04 géré par @PrePersist dans l'entité)
        Reservation reservation = new Reservation();
        reservation.setBookId(request.getBookId());
        reservation.setUserId(targetAdherentId);
        reservation.setStatus(ReservationStatus.EN_ATTENTE);

        Reservation saved = reservationRepository.save(reservation);
        return toResponse(saved, book, user);
    }

    @Override
    public List<ReservationResponse> getReservations(ReservationStatus status, Integer userId, Integer callerId, boolean bibliothecaire) {
        // RS-05 : un ADHERENT ne voit que ses propres réservations, quel que soit le
        // paramètre userId qu'il aurait pu passer — seul un BIBLIOTHECAIRE peut filtrer
        // sur l'adhérent de son choix (ou lister tout le monde en l'omettant).
        Integer effectiveUserId = bibliothecaire ? userId : callerId;

        List<Reservation> reservations;
        if (status != null && effectiveUserId != null) {
            reservations = reservationRepository.findByUserIdAndStatus(effectiveUserId, status);
        } else if (status != null) {
            reservations = reservationRepository.findByStatus(status);
        } else if (effectiveUserId != null) {
            reservations = reservationRepository.findByUserId(effectiveUserId);
        } else {
            reservations = reservationRepository.findAll();
        }
        return reservations.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ReservationResponse getReservationById(Integer id, Integer callerId, boolean bibliothecaire) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnedByCallerOrBibliothecaire(reservation, callerId, bibliothecaire);
        return toResponse(reservation);
    }

    @Override
    public ReservationResponse annulerReservation(Integer id, Integer callerId, boolean bibliothecaire) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnedByCallerOrBibliothecaire(reservation, callerId, bibliothecaire);

        // RG-06 : Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état
        // RG-05 : Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE
        if (reservation.getStatus() == ReservationStatus.ANNULEE) {
            throw new ConflictException("RG-06: Cette réservation ne peut pas être annulée car son statut est ANNULEE");
        }
        if (reservation.getStatus() == ReservationStatus.EXPIREE) {
            throw new ConflictException("RG-06: Cette réservation ne peut pas être annulée car son statut est EXPIREE");
        }
        if (reservation.getStatus() == ReservationStatus.HONOREE) {
            throw new ConflictException("RG-06: Cette réservation ne peut pas être annulée car son statut est HONOREE");
        }

        reservation.setStatus(ReservationStatus.ANNULEE);
        return toResponse(reservationRepository.save(reservation));
    }

    @Override
    public void deleteReservation(Integer id) {
        // Reserve au BIBLIOTHECAIRE : impose au niveau du contrôleur (@PreAuthorize).
        reservationRepository.delete(findReservationOrThrow(id));
    }

    private Reservation findReservationOrThrow(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Réservation non trouvée avec l'id: " + id));
    }

    /**
     * RS-03 : un ADHERENT qui accède à la réservation d'un autre reçoit 403.
     * Un BIBLIOTHECAIRE n'est jamais concerné par cette restriction.
     */
    private void assertOwnedByCallerOrBibliothecaire(Reservation reservation, Integer callerId, boolean bibliothecaire) {
        if (!bibliothecaire && !reservation.getUserId().equals(callerId)) {
            throw new AccessDeniedException("Cette réservation ne vous appartient pas");
        }
    }

    /** Résout bookTitle/userName par ID — utilisé quand on n'a pas déjà Books/Users sous la main. */
    private ReservationResponse toResponse(Reservation r) {
        String bookTitle = r.getBookId() != null
                ? booksRepository.findById(r.getBookId()).map(Books::getBookName).orElse(null)
                : null;
        String userName = r.getUserId() != null
                ? usersRepository.findById(r.getUserId()).map(Users::getName).orElse(null)
                : null;
        return buildResponse(r, bookTitle, userName);
    }

    /** Variante sans requêtes supplémentaires quand Books/Users sont déjà chargés (création). */
    private ReservationResponse toResponse(Reservation r, Books book, Users user) {
        return buildResponse(r, book.getBookName(), user.getName());
    }

    private ReservationResponse buildResponse(Reservation r, String bookTitle, String userName) {
        boolean active = r.getStatus() == ReservationStatus.EN_ATTENTE || r.getStatus() == ReservationStatus.DISPONIBLE;
        return new ReservationResponse(
                r.getReservationId(),
                r.getBookId(),
                bookTitle,
                r.getUserId(),
                userName,
                r.getDateReservation(),
                r.getDateExpiration(),
                r.getStatus(),
                active
        );
    }
}
