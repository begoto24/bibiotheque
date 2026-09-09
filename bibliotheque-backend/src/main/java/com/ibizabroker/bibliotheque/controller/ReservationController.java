package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.service.IReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Aucune logique métier ici : validation, règles de gestion et accès aux
 * données vivent dans ReservationServiceImpl. Les exceptions métier
 * (NotFoundException, ConflictException, IllegalArgumentException) sont
 * traduites en réponses HTTP par GlobalExceptionHandler — même pattern que
 * BooksController.
 */
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Gestion des réservations de livres")
public class ReservationController {

    @Autowired
    private IReservationService reservationService;

    @PostMapping
    @Operation(
            summary = "Créer une réservation",
            description = "Réserve un livre indisponible pour un adhérent. " +
                    "RG-01 : le livre doit être indisponible. " +
                    "RG-02 : une seule réservation active par livre/adhérent. " +
                    "RG-03 : max 3 réservations actives.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
                    @ApiResponse(responseCode = "400", description = "bookId ou adherentId manquant"),
                    @ApiResponse(responseCode = "404", description = "Livre ou utilisateur non trouvé"),
                    @ApiResponse(responseCode = "409", description = "Règle de gestion violée (RG-01, RG-02, RG-03)")
            })
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        ReservationResponse reservation = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping
    @Operation(
            summary = "Lister les réservations",
            description = "Retourne la liste des réservations, filtrable par statut et/ou adhérent.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Liste des réservations (vide si aucune)")
            })
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Integer userId) {
        return ResponseEntity.ok(reservationService.getReservations(status, userId));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consulter une réservation",
            description = "Retourne les détails d'une réservation par son ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
                    @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
            })
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @PatchMapping("/{id}/annuler")
    @Operation(
            summary = "Annuler une réservation",
            description = "Annule une réservation active. " +
                    "RG-05 : uniquement si statut EN_ATTENTE ou DISPONIBLE. " +
                    "RG-06 : impossible si statut ANNULEE, EXPIREE ou HONOREE.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Réservation annulée"),
                    @ApiResponse(responseCode = "404", description = "Réservation non trouvée"),
                    @ApiResponse(responseCode = "409", description = "Statut incompatible (RG-05, RG-06)")
            })
    public ResponseEntity<ReservationResponse> annulerReservation(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.annulerReservation(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(
            summary = "Supprimer une réservation",
            description = "Supprime définitivement une réservation. Rôle Admin requis.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Réservation supprimée"),
                    @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
            })
    public ResponseEntity<Void> deleteReservation(@PathVariable Integer id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
