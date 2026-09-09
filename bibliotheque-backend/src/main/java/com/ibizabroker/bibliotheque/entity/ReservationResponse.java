package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de sortie du module Réservation : c'est ce qui est réellement envoyé
 * au client. L'entité Reservation ne doit jamais sortir du service (voir
 * ReservationServiceImpl.toResponse) — bookTitle/userName sont résolus ici
 * côté serveur pour que le frontend n'ait pas à recouper les IDs lui-même.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Integer id;
    private Integer bookId;
    private String bookTitle;
    private Integer userId;
    private String userName;

    @JsonSerialize(using = JsonDataSerializer.class)
    private LocalDateTime reservationDate;

    @JsonSerialize(using = JsonDataSerializer.class)
    private LocalDateTime expirationDate;

    private ReservationStatus status;

    /** true si le statut est EN_ATTENTE ou DISPONIBLE (réservation active). */
    private boolean active;
}
