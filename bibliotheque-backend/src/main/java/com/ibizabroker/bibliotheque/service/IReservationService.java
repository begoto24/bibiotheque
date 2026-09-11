package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;

import java.util.List;

public interface IReservationService {

    /**
     * @param callerId       id de l'utilisateur authentifie (extrait du token, jamais du corps
     *                       de la requete)
     * @param bibliothecaire true si l'appelant a le role BIBLIOTHECAIRE (mappe depuis Admin)
     */
    ReservationResponse createReservation(ReservationRequest request, Integer callerId, boolean bibliothecaire);

    List<ReservationResponse> getReservations(ReservationStatus status, Integer userId, Integer callerId, boolean bibliothecaire);

    ReservationResponse getReservationById(Integer id, Integer callerId, boolean bibliothecaire);

    ReservationResponse annulerReservation(Integer id, Integer callerId, boolean bibliothecaire);

    void deleteReservation(Integer id);
}
