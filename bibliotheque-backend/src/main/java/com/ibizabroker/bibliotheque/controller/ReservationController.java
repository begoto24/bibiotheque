package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.ReservationStatut;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Créer une réservation")
    public ResponseEntity<ReservationResponseDTO> creer(@RequestBody ReservationRequestDTO request) {
        ReservationResponseDTO creee = reservationService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creee);
    }

    @GetMapping
    @Operation(summary = "Lister les réservations")
    public ResponseEntity<List<ReservationResponseDTO>> lister(
            @RequestParam(required = false) ReservationStatut statut,
            @RequestParam(required = false) Integer adherentId) {
        return ResponseEntity.ok(reservationService.lister(statut, adherentId));
    }

    @GetMapping("/expirees")
    @Operation(summary = "Lister les réservations expirées")
    public ResponseEntity<List<ReservationResponseDTO>> listerExpirees() {
        return ResponseEntity.ok(reservationService.listerExpirees());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une réservation")
    public ResponseEntity<ReservationResponseDTO> consulter(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.consulter(id));
    }

    @PatchMapping("/{id}/annuler")
    @Operation(summary = "Annuler une réservation")
    public ResponseEntity<ReservationResponseDTO> annuler(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.annuler(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une réservation")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        reservationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
