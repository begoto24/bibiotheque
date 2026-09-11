package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.IReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Aucune logique métier ici : validation, règles de gestion, règles de sécurité
 * objet (RS-03/RS-04/RS-05) et accès aux données vivent dans ReservationServiceImpl.
 * Les exceptions métier sont traduites en réponses HTTP par GlobalExceptionHandler —
 * même pattern que BooksController.
 *
 * Autorisations (séance 4) : ANONYME toujours 401 (WebSecurityConfiguration exige
 * l'authentification sur /api/reservations/**). ADHERENT et BIBLIOTHECAIRE peuvent
 * tous les deux créer/consulter/annuler, mais la portée (soi-même vs tout le monde)
 * est appliquée au niveau service à partir de l'identité extraite du token — jamais
 * du corps de la requête (RS-04). Seul BIBLIOTHECAIRE peut supprimer.
 */
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Gestion des réservations de livres")
public class ReservationController {

    @Autowired
    private IReservationService reservationService;

    @Autowired
    private UsersRepository usersRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    @Operation(
            summary = "Créer une réservation",
            description = "Réserve un livre indisponible. Un ADHERENT ne peut réserver que pour lui-même " +
                    "(adherentId du corps ignoré, l'identité vient du token). Un BIBLIOTHECAIRE peut réserver " +
                    "pour n'importe quel adhérent. RG-01 à RG-03 s'appliquent dans les deux cas.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
                    @ApiResponse(responseCode = "400", description = "bookId (ou adherentId pour un bibliothécaire) manquant"),
                    @ApiResponse(responseCode = "401", description = "Non authentifié"),
                    @ApiResponse(responseCode = "404", description = "Livre ou utilisateur non trouvé"),
                    @ApiResponse(responseCode = "409", description = "Règle de gestion violée (RG-01, RG-02, RG-03)")
            })
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request, Authentication authentication) {
        Users caller = resolveCaller(authentication);
        ReservationResponse reservation = reservationService.createReservation(request, caller.getUserId(), isBibliothecaire(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    @Operation(
            summary = "Lister les réservations",
            description = "Un ADHERENT ne voit que ses propres réservations (RS-05), quel que soit le " +
                    "paramètre userId envoyé. Un BIBLIOTHECAIRE voit tout, filtrable par statut et/ou adhérent.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Liste des réservations (vide si aucune)"),
                    @ApiResponse(responseCode = "401", description = "Non authentifié")
            })
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Integer userId,
            Authentication authentication) {
        Users caller = resolveCaller(authentication);
        return ResponseEntity.ok(reservationService.getReservations(status, userId, caller.getUserId(), isBibliothecaire(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    @Operation(
            summary = "Consulter une réservation",
            description = "Un ADHERENT ne peut consulter que sa propre réservation (RS-03).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
                    @ApiResponse(responseCode = "401", description = "Non authentifié"),
                    @ApiResponse(responseCode = "403", description = "Réservation d'un autre adhérent"),
                    @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
            })
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Integer id, Authentication authentication) {
        Users caller = resolveCaller(authentication);
        return ResponseEntity.ok(reservationService.getReservationById(id, caller.getUserId(), isBibliothecaire(authentication)));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    @Operation(
            summary = "Annuler une réservation",
            description = "Un ADHERENT ne peut annuler que sa propre réservation (RS-03). " +
                    "RG-05 : uniquement si statut EN_ATTENTE ou DISPONIBLE. " +
                    "RG-06 : impossible si statut ANNULEE, EXPIREE ou HONOREE.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Réservation annulée"),
                    @ApiResponse(responseCode = "401", description = "Non authentifié"),
                    @ApiResponse(responseCode = "403", description = "Réservation d'un autre adhérent"),
                    @ApiResponse(responseCode = "404", description = "Réservation non trouvée"),
                    @ApiResponse(responseCode = "409", description = "Statut incompatible (RG-05, RG-06)")
            })
    public ResponseEntity<ReservationResponse> annulerReservation(@PathVariable Integer id, Authentication authentication) {
        Users caller = resolveCaller(authentication);
        return ResponseEntity.ok(reservationService.annulerReservation(id, caller.getUserId(), isBibliothecaire(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    @Operation(
            summary = "Supprimer une réservation",
            description = "Supprime définitivement une réservation. Réservé au BIBLIOTHECAIRE (jamais l'ADHERENT).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Réservation supprimée"),
                    @ApiResponse(responseCode = "401", description = "Non authentifié"),
                    @ApiResponse(responseCode = "403", description = "Réservé au bibliothécaire"),
                    @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
            })
    public ResponseEntity<Void> deleteReservation(@PathVariable Integer id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    private Users resolveCaller(Authentication authentication) {
        return usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Utilisateur authentifié introuvable: " + authentication.getName()));
    }

    private boolean isBibliothecaire(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTHECAIRE"));
    }
}
