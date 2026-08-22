package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.ReservationStatut;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDTO {
    private Integer id;
    private Integer livreId;
    private Integer adherentId;
    private LocalDateTime dateReservation;
    private LocalDateTime dateExpiration;
    private ReservationStatut statut;
}
