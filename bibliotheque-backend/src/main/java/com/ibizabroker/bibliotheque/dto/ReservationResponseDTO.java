package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.ReservationStatut;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReservationResponseDTO {
    private Integer id;
    private Integer livreId;
    private Integer adherentId;
    private LocalDateTime dateReservation;
    private LocalDateTime dateExpiration;
    private ReservationStatut statut;
}
