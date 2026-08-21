package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.ReservationStatut;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceRg03Test {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Books livre;
    private Users adherent;
    private ReservationRequestDTO request;

    @BeforeEach
    void setUp() {
        livre = new Books();
        livre.setBookId(10);
        livre.setNoOfCopies(0);

        adherent = new Users();
        adherent.setUserId(20);

        request = new ReservationRequestDTO();
        request.setLivreId(10);
        request.setAdherentId(20);
    }

    @Test
    void creer_refuse_quand_adherent_a_deja_3_reservations_actives_RG03() {
        when(reservationRepository.findByStatutInAndDateExpirationBefore(anyList(), any()))
                .thenReturn(Collections.emptyList());
        when(booksRepository.findById(10)).thenReturn(Optional.of(livre));
        when(usersRepository.findById(20)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByAdherent_UserIdAndLivre_BookIdAndStatutIn(
                eq(20), eq(10), anyList())).thenReturn(false);
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(
                eq(20),
                eq(Arrays.asList(ReservationStatut.EN_ATTENTE, ReservationStatut.DISPONIBLE))))
                .thenReturn(3L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.creer(request));

        assertTrue(ex.getMessage().contains("RG-03"));
    }
}
