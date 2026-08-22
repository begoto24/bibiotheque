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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    public ReservationResponseDTO creer(ReservationRequestDTO request) {
        expirerReservationsDepassees();

        if (request.getLivreId() == null) {
            throw new BadRequestException("livreId is required");
        }
        if (request.getAdherentId() == null) {
            throw new BadRequestException("adherentId is required");
        }

        Books livre = booksRepository.findById(request.getLivreId())
                .orElseThrow(() -> new NotFoundException(
                        "Book with id " + request.getLivreId() + " not found."));
        Users adherent = usersRepository.findById(request.getAdherentId())
                .orElseThrow(() -> new NotFoundException(
                        "User with id " + request.getAdherentId() + " not found."));

        if (livre.getNoOfCopies() != null && livre.getNoOfCopies() > 0) {
            throw new ConflictException(
                    "RG-01: reservation is only allowed for unavailable books.");
        }

        if (reservationRepository.existsByAdherent_UserIdAndLivre_BookIdAndStatutIn(
                adherent.getUserId(), livre.getBookId(),
                Arrays.asList(ReservationStatut.EN_ATTENTE, ReservationStatut.DISPONIBLE))) {
            throw new ConflictException(
                    "RG-02: an adherent can only have one active reservation per book.");
        }

        long actives = reservationRepository.countByAdherent_UserIdAndStatutIn(
                adherent.getUserId(),
                Arrays.asList(ReservationStatut.EN_ATTENTE, ReservationStatut.DISPONIBLE));
        if (actives >= 3) {
            throw new ConflictException(
                    "RG-03: an adherent cannot exceed 3 active reservations.");
        }

        LocalDateTime maintenant = LocalDateTime.now();
        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(maintenant);
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

    public List<ReservationResponseDTO> listerExpirees() {
        return reservationRepository.findByStatut(ReservationStatut.EXPIREE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ReservationResponseDTO consulter(Integer id) {
        return toDto(trouverOu404(id));
    }

    public ReservationResponseDTO annuler(Integer id) {
        Reservation reservation = trouverOu404(id);

        if (Arrays.asList(ReservationStatut.ANNULEE, ReservationStatut.EXPIREE, ReservationStatut.HONOREE)
                .contains(reservation.getStatut())) {
            throw new ConflictException(
                    "RG-06: a reservation with status " + reservation.getStatut() + " cannot be changed.");
        }

        if (!Arrays.asList(ReservationStatut.EN_ATTENTE, ReservationStatut.DISPONIBLE)
                .contains(reservation.getStatut())) {
            throw new ConflictException(
                    "RG-05: a reservation can only be cancelled if its status is EN_ATTENTE or DISPONIBLE.");
        }

        reservation.setStatut(ReservationStatut.ANNULEE);
        return toDto(reservationRepository.save(reservation));
    }

    public void supprimer(Integer id) {
        Reservation reservation = trouverOu404(id);
        reservationRepository.delete(reservation);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void expirerReservationsDepassees() {
        List<Reservation> aExpirer = reservationRepository
                .findByStatutInAndDateExpirationBefore(
                        Arrays.asList(ReservationStatut.EN_ATTENTE, ReservationStatut.DISPONIBLE),
                        LocalDateTime.now());
        for (Reservation reservation : aExpirer) {
            reservation.setStatut(ReservationStatut.EXPIREE);
        }
        if (!aExpirer.isEmpty()) {
            reservationRepository.saveAll(aExpirer);
        }
    }

    private Reservation trouverOu404(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Reservation with id " + id + " not found."));
    }

    private ReservationResponseDTO toDto(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setLivreId(reservation.getLivre().getBookId());
        dto.setAdherentId(reservation.getAdherent().getUserId());
        dto.setDateReservation(reservation.getDateReservation());
        dto.setDateExpiration(reservation.getDateExpiration());
        dto.setStatut(reservation.getStatut());
        return dto;
    }
}
