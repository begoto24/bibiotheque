package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    long countByAdherent_UserIdAndStatutIn(Integer adherentId, List<ReservationStatut> statuts);

    boolean existsByAdherent_UserIdAndLivre_BookIdAndStatutIn(
            Integer adherentId, Integer livreId, List<ReservationStatut> statuts);

    List<Reservation> findByStatut(ReservationStatut statut);

    List<Reservation> findByAdherent_UserId(Integer adherentId);

    List<Reservation> findByStatutAndAdherent_UserId(ReservationStatut statut, Integer adherentId);

    List<Reservation> findByStatutInAndDateExpirationBefore(
            List<ReservationStatut> statuts, LocalDateTime date);
}
