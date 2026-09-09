package com.ibizabroker.bibliotheque.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class Users {

    @Id
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username")
    private String username;

    @Column(name = "name")
    private String name;

    @Column(name = "password")
    private String password;

    // Pas de cascade : Role est une donnee de reference partagee (Admin/User),
    // pas possedee par un seul utilisateur. Avec CascadeType.ALL (incluant
    // REMOVE), supprimer UN utilisateur supprimait la ligne "role" elle-meme,
    // ce qui faisait perdre le role de TOUS les autres utilisateurs qui la
    // partageaient (ON DELETE CASCADE sur user_role.role_id propage ensuite).
    // Sans cascade, JPA continue de gerer la table de jointure user_role
    // normalement ; seule la propagation vers l'entite Role elle-meme est coupee.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = {
                    @JoinColumn(name = "user_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "role_id")
            }
    )
    private Set<Role> role;

}
