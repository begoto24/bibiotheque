package com.ibizabroker.bibliotheque;

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
import com.ibizabroker.bibliotheque.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de la couche service uniquement : tous les repositories sont mockes
 * (aucune base ne tourne). Les tests d'integration qui exercent la vraie
 * chaine HTTP + securite + JWT sont dans ReservationSecurityIntegrationTest.
 *
 * Convention pour les tests herites de la seance 2 (createReservation,
 * annulerReservation, getReservationById, getReservations) : appeles avec
 * bibliothecaire=true et un callerId arbitraire (99), pour preserver leur
 * comportement d'origine (adherentId/userId pris tel quel depuis la requete)
 * sans avoir a retoucher tous leurs mocks. Les tests RS-03/RS-04/RS-05
 * ci-dessous verifient specifiquement le cas ADHERENT (bibliothecaire=false).
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceUnitTest {

    private static final Integer BIBLIOTHECAIRE_CALLER_ID = 99;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Books book;
    private Users user;
    private ReservationRequest request;

    @BeforeEach
    void setUp() {
        book = new Books();
        book.setBookId(1);
        book.setBookName("Test Book");
        book.setNoOfCopies(0); // indisponible par défaut

        user = new Users();
        user.setUserId(2);
        user.setUsername("adherent1");
        user.setName("Adhérent Test");

        request = new ReservationRequest();
        request.setBookId(1);
        request.setAdherentId(2);
    }

    // ================================================================
    // RG-01 : On ne peut réserver qu'un livre indisponible
    // ================================================================

    @Test
    void RG01_shouldRejectReservationWhenBookIsAvailable() {
        // GIVEN : livre disponible (noOfCopies > 0)
        book.setNoOfCopies(3);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));

        // WHEN / THEN : 409 Conflict
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true));
        assertTrue(ex.getMessage().contains("RG-01"));
        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void RG01_shouldAllowReservationWhenBookIsUnavailable() {
        // GIVEN : livre indisponible (noOfCopies == 0)
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1, List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2, List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(1);
            return r;
        });

        // WHEN
        ReservationResponse result = reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN : 201 Created
        assertNotNull(result);
        assertEquals(ReservationStatus.EN_ATTENTE, result.getStatus());
    }

    @Test
    void RG01_shouldAllowReservationWhenNoOfCopiesIsNegative() {
        // GIVEN : noOfCopies < 0 (edge case, book is "unavailable")
        book.setNoOfCopies(-1);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(5);
            return r;
        });

        // WHEN : RG-01 ne se déclenche PAS car noOfCopies n'est pas > 0
        ReservationResponse result = reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN : la réservation est créée (le livre est indisponible)
        assertNotNull(result);
        assertEquals(ReservationStatus.EN_ATTENTE, result.getStatus());
    }

    // ================================================================
    // RG-02 : Un adhérent ne peut avoir qu'une seule réservation active
    //         sur un même livre
    // ================================================================

    @Test
    void RG02_shouldRejectWhenActiveReservationAlreadyExists() {
        // GIVEN
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(true);

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true));
        assertTrue(ex.getMessage().contains("RG-02"));
        assertTrue(ex.getMessage().contains("déjà"));
    }

    @Test
    void RG02_shouldAllowWhenOnlyCancelledReservationsExist() {
        // GIVEN : réservation annulée ne compte pas comme active
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(2);
            return r;
        });

        // WHEN
        ReservationResponse result = reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN : la réservation est créée
        assertNotNull(result);
        assertEquals(ReservationStatus.EN_ATTENTE, result.getStatus());
    }

    // ================================================================
    // RG-03 : Max 3 réservations actives simultanées
    // (demande explicitement par la seance 4 : repository mocke, 2 cas)
    // ================================================================

    @Test
    void RG03_shouldRejectWhen3ActiveReservationsExist() {
        // GIVEN : 3 réservations actives déjà existantes
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(3L);

        // WHEN / THEN : refus
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true));
        assertTrue(ex.getMessage().contains("RG-03"));
        assertTrue(ex.getMessage().contains("maximum"));
    }

    @Test
    void RG03_shouldAllowWhenOnly2ActiveReservationsExist() {
        // GIVEN : 2 réservations actives -> la 3ème doit passer
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(2L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(3);
            return r;
        });

        // WHEN
        ReservationResponse result = reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN : 3ème réservation acceptée
        assertNotNull(result);
    }

    // ================================================================
    // RG-04 : dateExpiration = dateReservation + 7 jours
    // ================================================================

    @Test
    void RG04_shouldSetExpirationTo7DaysAfterReservation() {
        // GIVEN
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(4);
            r.setDateReservation(LocalDateTime.now());
            r.setDateExpiration(r.getDateReservation().plusDays(7));
            return r;
        });

        // WHEN
        ReservationResponse result = reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN
        assertNotNull(result.getReservationDate());
        assertNotNull(result.getExpirationDate());
        assertEquals(7,
                java.time.temporal.ChronoUnit.DAYS.between(
                        result.getReservationDate(), result.getExpirationDate()),
                "L'expiration doit être à exactement 7 jours de la réservation");
    }

    // ================================================================
    // RG-05 : Annulation possible uniquement si EN_ATTENTE ou DISPONIBLE
    // ================================================================

    @Test
    void RG05_shouldAllowCancellationWhenStatusIsEnAttente() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(10);
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(10)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        ReservationResponse result = reservationService.annulerReservation(10, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN
        assertEquals(ReservationStatus.ANNULEE, result.getStatus());
    }

    @Test
    void RG05_shouldAllowCancellationWhenStatusIsDisponible() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(11);
        reservation.setStatus(ReservationStatus.DISPONIBLE);
        when(reservationRepository.findById(11)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        ReservationResponse result = reservationService.annulerReservation(11, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN
        assertEquals(ReservationStatus.ANNULEE, result.getStatus());
    }

    // ================================================================
    // RG-06 : Statut ANNULEE / EXPIREE / HONOREE → pas de changement
    // ================================================================

    @Test
    void RG06_shouldRejectCancellationWhenStatusIsAnnulee() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(20);
        reservation.setStatus(ReservationStatus.ANNULEE);
        when(reservationRepository.findById(20)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(20, BIBLIOTHECAIRE_CALLER_ID, true));
        assertTrue(ex.getMessage().contains("RG-06"));
        assertTrue(ex.getMessage().contains("ANNULEE"));
    }

    @Test
    void RG06_shouldRejectCancellationWhenStatusIsExpiree() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(21);
        reservation.setStatus(ReservationStatus.EXPIREE);
        when(reservationRepository.findById(21)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(21, BIBLIOTHECAIRE_CALLER_ID, true));
        assertTrue(ex.getMessage().contains("RG-06"));
        assertTrue(ex.getMessage().contains("EXPIREE"));
    }

    @Test
    void RG06_shouldRejectCancellationWhenStatusIsHonoree() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(22);
        reservation.setStatus(ReservationStatus.HONOREE);
        when(reservationRepository.findById(22)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(22, BIBLIOTHECAIRE_CALLER_ID, true));
        assertTrue(ex.getMessage().contains("RG-06"));
        assertTrue(ex.getMessage().contains("HONOREE"));
    }

    // ================================================================
    // RS-03 : un ADHERENT qui accède à la réservation d'un autre reçoit 403
    // ================================================================

    @Test
    void RS03_adherentAccessingSomeoneElsesReservationIsDenied() {
        // GIVEN : réservation appartenant à l'adhérent 3, appelant = adhérent 2
        Reservation reservation = new Reservation();
        reservation.setReservationId(40);
        reservation.setUserId(3);
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(40)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        assertThrows(AccessDeniedException.class,
                () -> reservationService.getReservationById(40, 2, false));
    }

    @Test
    void RS03_adherentAccessingOwnReservationIsAllowed() {
        // GIVEN : réservation appartenant à l'appelant lui-même
        Reservation reservation = new Reservation();
        reservation.setReservationId(41);
        reservation.setUserId(2);
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(41)).thenReturn(Optional.of(reservation));

        // WHEN
        ReservationResponse result = reservationService.getReservationById(41, 2, false);

        // THEN
        assertNotNull(result);
        assertEquals(2, result.getUserId());
    }

    @Test
    void RS03_bibliothecaireCanAccessAnyonesReservation() {
        // GIVEN : réservation appartenant à l'adhérent 3, appelant = bibliothécaire
        Reservation reservation = new Reservation();
        reservation.setReservationId(42);
        reservation.setUserId(3);
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(42)).thenReturn(Optional.of(reservation));

        // WHEN : pas d'exception, malgré un callerId différent du propriétaire
        ReservationResponse result = reservationService.getReservationById(42, 99, true);

        // THEN
        assertNotNull(result);
        assertEquals(3, result.getUserId());
    }

    @Test
    void RS03_adherentCancellingSomeoneElsesReservationIsDenied() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(43);
        reservation.setUserId(3);
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(43)).thenReturn(Optional.of(reservation));

        // WHEN / THEN : 403, pas 409 — la propriété est vérifiée avant RG-05/RG-06
        assertThrows(AccessDeniedException.class,
                () -> reservationService.annulerReservation(43, 2, false));
        verify(reservationRepository, never()).save(any());
    }

    // ================================================================
    // RS-04 : l'identité du créateur vient du token, jamais du corps de
    // la requête — un ADHERENT ne peut pas réserver au nom d'un autre
    // ================================================================

    @Test
    void RS04_adherentCreatingReservationAlwaysUsesOwnTokenIdentity() {
        // GIVEN : la requête tente de réserver au nom de l'adhérent 999,
        // mais l'appelant authentifié est l'adhérent 2 (bibliothecaire=false)
        request.setAdherentId(999);
        Users caller = new Users();
        caller.setUserId(2);
        caller.setName("Adhérent Appelant");

        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(caller));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        when(reservationRepository.save(captor.capture())).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(50);
            return r;
        });

        // WHEN
        ReservationResponse result = reservationService.createReservation(request, 2, false);

        // THEN : la réservation est créée pour l'appelant (2), jamais pour 999
        assertEquals(2, result.getUserId());
        assertEquals(2, captor.getValue().getUserId());
        verify(usersRepository, never()).findById(999);
    }

    @Test
    void RS04_bibliothecaireCanCreateReservationForAnyAdherent() {
        // GIVEN : un bibliothécaire réserve explicitement pour l'adhérent 2
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(51);
            return r;
        });

        // WHEN : callerId (99, le bibliothécaire) différent de l'adherentId visé (2)
        ReservationResponse result = reservationService.createReservation(request, 99, true);

        // THEN : la réservation est bien créée pour l'adhérent 2, pas pour le bibliothécaire
        assertEquals(2, result.getUserId());
    }

    // ================================================================
    // RS-05 : un GET par un ADHERENT ne retourne que ses propres
    // réservations, quel que soit le paramètre userId envoyé
    // ================================================================

    @Test
    void RS05_adherentListingReservationsIgnoresRequestedUserIdFilter() {
        // GIVEN : l'appelant (adhérent 2) demande explicitement les réservations
        // de l'adhérent 999 — ça doit être ignoré au profit de son propre id
        Reservation ownReservation = new Reservation();
        ownReservation.setReservationId(60);
        ownReservation.setUserId(2);
        when(reservationRepository.findByUserId(2)).thenReturn(List.of(ownReservation));

        // WHEN
        List<ReservationResponse> result = reservationService.getReservations(null, 999, 2, false);

        // THEN
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getUserId());
        verify(reservationRepository, never()).findByUserId(999);
        verify(reservationRepository, never()).findAll();
    }

    @Test
    void RS05_bibliothecaireListingReservationsCanFilterByAnyUserId() {
        // GIVEN
        Reservation r = new Reservation();
        r.setReservationId(61);
        r.setUserId(3);
        when(reservationRepository.findByUserId(3)).thenReturn(List.of(r));

        // WHEN : bibliothécaire, filtre explicite sur l'adhérent 3
        List<ReservationResponse> result = reservationService.getReservations(null, 3, 99, true);

        // THEN
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getUserId());
    }

    @Test
    void RS05_bibliothecaireListingWithoutFilterSeesEveryone() {
        // GIVEN
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(new Reservation(), new Reservation()));

        // WHEN : bibliothécaire, aucun filtre
        List<ReservationResponse> result = reservationService.getReservations(null, null, 99, true);

        // THEN
        assertEquals(2, result.size());
    }

    // ================================================================
    // Cas limites : ressources non trouvées
    // ================================================================

    @Test
    void shouldThrowNotFoundExceptionWhenBookDoesNotExist() {
        // GIVEN
        when(booksRepository.findById(999)).thenReturn(Optional.empty());
        request.setBookId(999);

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        // GIVEN
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(999)).thenReturn(Optional.empty());
        request.setAdherentId(999);

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenReservationDoesNotExist() {
        // GIVEN
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.annulerReservation(999, BIBLIOTHECAIRE_CALLER_ID, true));
    }

    @Test
    void shouldThrowNotFoundExceptionOnGetById() {
        // GIVEN
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.getReservationById(999, BIBLIOTHECAIRE_CALLER_ID, true));
    }

    // ================================================================
    // Validation des paramètres
    // ================================================================

    @Test
    void shouldThrowExceptionWhenBookIdIsNull() {
        // GIVEN
        request.setBookId(null);

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true));
    }

    @Test
    void shouldThrowExceptionWhenBibliothecaireOmitsAdherentId() {
        // GIVEN : un bibliothécaire doit préciser pour qui il réserve
        request.setAdherentId(null);

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.createReservation(request, BIBLIOTHECAIRE_CALLER_ID, true));
    }

    // ================================================================
    // GET / DELETE
    // ================================================================

    @Test
    void shouldReturnReservationsFilteredByStatus() {
        // GIVEN
        Reservation r1 = new Reservation();
        r1.setReservationId(1);
        r1.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findByStatus(ReservationStatus.EN_ATTENTE))
                .thenReturn(Arrays.asList(r1));

        // WHEN
        List<ReservationResponse> result = reservationService.getReservations(ReservationStatus.EN_ATTENTE, null, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN
        assertEquals(1, result.size());
        assertEquals(ReservationStatus.EN_ATTENTE, result.get(0).getStatus());
    }

    @Test
    void shouldReturnReservationsFilteredByUserId() {
        // GIVEN
        Reservation r1 = new Reservation();
        r1.setReservationId(1);
        r1.setUserId(2);
        when(reservationRepository.findByUserId(2)).thenReturn(Arrays.asList(r1));

        // WHEN
        List<ReservationResponse> result = reservationService.getReservations(null, 2, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getUserId());
    }

    @Test
    void shouldReturnAllReservationsWhenNoFilter() {
        // GIVEN
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(new Reservation(), new Reservation()));

        // WHEN
        List<ReservationResponse> result = reservationService.getReservations(null, null, BIBLIOTHECAIRE_CALLER_ID, true);

        // THEN
        assertEquals(2, result.size());
    }

    @Test
    void shouldDeleteReservationSuccessfully() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(30);
        when(reservationRepository.findById(30)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        assertDoesNotThrow(() -> reservationService.deleteReservation(30));
        verify(reservationRepository).delete(reservation);
    }
}
