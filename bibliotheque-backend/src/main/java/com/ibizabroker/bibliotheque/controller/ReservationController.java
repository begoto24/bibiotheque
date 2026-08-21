package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.ReservationStatut;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Module de réservation de livres")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @Operation(
            summary = "Créer une réservation",
            description = "Le client envoie seulement livreId et adherentId. "
                    + "Le livre doit être indisponible (noOfCopies = 0). "
                    + "En erreur, le corps contient { status, message }.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée (statut EN_ATTENTE, expiration +7 jours)"),
            @ApiResponse(responseCode = "400", description = "Ex. message: \"livreId manquant\" ou \"adherentId manquant\""),
            @ApiResponse(responseCode = "404", description = "Ex. message: \"Livre avec id … introuvable.\""),
            @ApiResponse(responseCode = "409", description = "Ex. message: \"RG-01 : on ne peut réserver qu'un livre indisponible.\" "
                    + "(aussi RG-02 / RG-03 selon le cas)")
    })
    public ResponseEntity<ReservationResponseDTO> creer(@RequestBody ReservationRequestDTO request) {
        ReservationResponseDTO creee = reservationService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creee);
    }

    @GetMapping
    @Operation(summary = "Lister les réservations (filtrable par statut et adhérent)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des réservations")
    })
    public ResponseEntity<List<ReservationResponseDTO>> lister(
            @RequestParam(required = false) ReservationStatut statut,
            @RequestParam(required = false) Integer adherentId) {
        return ResponseEntity.ok(reservationService.lister(statut, adherentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une réservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<ReservationResponseDTO> consulter(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.consulter(id));
    }

    @PatchMapping("/{id}/annuler")
    @Operation(
            summary = "Annuler une réservation",
            description = "Possible seulement si statut EN_ATTENTE ou DISPONIBLE (RG-05). "
                    + "ANNULEE / EXPIREE / HONOREE → 409 (RG-06).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation passée en ANNULEE"),
            @ApiResponse(responseCode = "404", description = "Ex. message: \"Réservation avec id … introuvable.\""),
            @ApiResponse(responseCode = "409", description = "Ex. message: \"RG-05 : …\" ou \"RG-06 : …\"")
    })
    public ResponseEntity<ReservationResponseDTO> annuler(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.annuler(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une réservation")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation supprimée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        reservationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
