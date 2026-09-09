package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;

import java.util.List;

public interface IReservationService {

    ReservationResponse createReservation(ReservationRequest request);

    List<ReservationResponse> getReservations(ReservationStatus status, Integer userId);

    ReservationResponse getReservationById(Integer id);

    ReservationResponse annulerReservation(Integer id);

    void deleteReservation(Integer id);
}
