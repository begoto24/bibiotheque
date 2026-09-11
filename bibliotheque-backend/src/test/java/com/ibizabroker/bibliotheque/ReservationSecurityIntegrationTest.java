package com.ibizabroker.bibliotheque;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test d'intégration (séance 4, partie 2) : exerce la vraie chaîne HTTP ->
 * Spring Security -> JWT -> contrôleur -> service -> base réelle (Testcontainers,
 * même pattern que BibliothequeApplicationTests). Aucun mock ici — c'est ce qui
 * distingue ce test de ReservationServiceUnitTest.
 *
 * Vérifie sur GET /api/reservations (et GET /api/reservations/{id} pour le
 * troisième cas, qui est le seul endroit où "la réservation d'un autre" a un
 * sens — la liste, elle, se contente de filtrer sur RS-05) :
 *   - sans token -> 401
 *   - avec un token ADHERENT -> 200
 *   - avec un token ADHERENT, sur la réservation d'un autre adhérent -> 403
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReservationSecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // static : @Testcontainers + un conteneur static impose le cycle de vie JUnit
    // par défaut (nouvelle instance de test par méthode) — @TestInstance(PER_CLASS)
    // casse l'ordre de démarrage du conteneur avec @DynamicPropertySource. Le seed
    // ne doit donc se faire qu'une fois, via un garde statique dans @BeforeEach.
    private static boolean seeded = false;
    private static String adherentAToken;
    private static Integer adherentBReservationId;

    @BeforeEach
    void seedDataOnce() {
        if (seeded) {
            return;
        }

        Role userRole = roleRepository.findByRoleName("User")
                .orElseGet(() -> roleRepository.save(newRole("User")));

        Users adherentA = usersRepository.save(newAdherent("adherentA_it", userRole));
        Users adherentB = usersRepository.save(newAdherent("adherentB_it", userRole));

        Books book = new Books();
        book.setBookName("Livre IT séance 4");
        book.setBookAuthor("Auteur IT");
        book.setBookGenre("Test");
        book.setNoOfCopies(0);
        book = booksRepository.save(book);

        // Réservation appartenant à l'adhérent B — utilisée pour vérifier le 403
        // quand l'adhérent A essaie d'y accéder.
        Reservation reservation = new Reservation();
        reservation.setBookId(book.getBookId());
        reservation.setUserId(adherentB.getUserId());
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        reservation = reservationRepository.save(reservation);
        this.adherentBReservationId = reservation.getReservationId();

        adherentAToken = authenticate("adherentA_it", "password123");
        seeded = true;
    }

    private Role newRole(String name) {
        Role role = new Role();
        role.setRoleName(name);
        return role;
    }

    private Users newAdherent(String username, Role role) {
        Users user = new Users();
        user.setUsername(username);
        user.setName(username);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Set.of(role));
        return user;
    }

    private String authenticate(String username, String password) {
        JwtRequest request = new JwtRequest();
        request.setUsername(username);
        request.setPassword(password);
        ResponseEntity<JwtResponse> response = restTemplate.postForEntity("/authenticate", request, JwtResponse.class);
        return response.getBody().getJwtToken();
    }

    @Test
    void getReservations_sansToken_renvoie401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/reservations", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getReservations_avecTokenAdherent_renvoie200() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adherentAToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reservations", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getReservationById_avecTokenAdherent_surReservationDunAutre_renvoie403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adherentAToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/reservations/" + adherentBReservationId, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
