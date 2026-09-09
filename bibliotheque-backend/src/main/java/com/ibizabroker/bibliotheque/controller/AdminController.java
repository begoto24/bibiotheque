package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin")
@Tag(name = "Administration", description = "Gestion des utilisateurs par l'administrateur")
public class AdminController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/users")
    @Operation(summary = "Créer un utilisateur", description = "Crée un nouvel utilisateur. Le mot de passe est automatiquement chiffré. Rôle Admin requis.")
    public Users addUserByAdmin(@RequestBody Users user) {
        user.setRole(resolveRoles(user.getRole()));
        String encryptPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptPassword);
        return usersRepository.save(user);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Lister tous les utilisateurs", description = "Retourne la liste de tous les utilisateurs. Rôle Admin requis.")
    public List<Users> getAllUsers() {
        return usersRepository.findAll();
    }

    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/users/{id}")
    @Operation(summary = "Consulter un utilisateur par ID", description = "Retourne les détails d'un utilisateur. Rôle Admin requis.")
    public ResponseEntity<Users> getUserById(@PathVariable Integer id) {
        Users user = usersRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id "+ id +" does not exist."));
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/users/{id}")
    @Operation(summary = "Modifier un utilisateur", description = "Met à jour les informations d'un utilisateur. Rôle Admin requis.")
    public ResponseEntity<Users> updateUser(@PathVariable Integer id, @RequestBody Users userDetails) {
        Users user = usersRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id "+ id +" does not exist."));

        user.setName(userDetails.getName());
        user.setUsername(userDetails.getUsername());
        user.setRole(resolveRoles(userDetails.getRole()));

        Users updatedUser = usersRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Supprimer un utilisateur", description = "Supprime définitivement un utilisateur. Rôle Admin requis.")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable Integer id) {
        Users user = usersRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id " + id + " does not exist."));

        usersRepository.delete(user);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    /**
     * Remplace les rôles envoyés par le client (qui n'ont qu'un roleName, pas
     * de roleId) par les rôles réellement persistés. Sans ça, le cascade ALL
     * sur Users.role tente de ré-insérer "Admin"/"User" et viole la contrainte
     * UNIQUE(role_name) — c'est ce qui faisait planter toute création/modification.
     */
    private Set<Role> resolveRoles(Set<Role> requestedRoles) {
        Set<Role> resolved = new HashSet<>();
        if (requestedRoles == null) {
            return resolved;
        }
        for (Role requested : requestedRoles) {
            Role role = roleRepository.findByRoleName(requested.getRoleName())
                    .orElseThrow(() -> new NotFoundException("Rôle inconnu: " + requested.getRoleName()));
            resolved.add(role);
        }
        return resolved;
    }
}
