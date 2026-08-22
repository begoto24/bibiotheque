<h1 align="center">
    <br>
    Bibliothèque
    <br>
</h1>

[![Spring Boot](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)]()
[![Angular](https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)]()
[![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white)]()
[![Maven](https://img.shields.io/badge/apache_maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)]()
[![Bootstrap](https://img.shields.io/badge/Bootstrap-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white)]()

Application full-stack de gestion de bibliothèque : **Spring Boot** (API REST) +
**Angular** (interface) + **PostgreSQL** (persistance).

* Deux profils : **Admin** (CRUD livres et utilisateurs) et **User** (emprunter / rendre).
* Module **Réservation** : réserver un livre indisponible (RG-01 à RG-06).
* Authentification par **JWT**.
* Mots de passe chiffrés avec **BCrypt**.
* Redirection vers une page *forbidden* si le rôle n'a pas accès à l'URL.

---

## Sommaire

1. [Prérequis](#1-prérequis)
2. [Arborescence](#2-arborescence)
3. [Démarrer le projet](#3-démarrer-le-projet)
4. [Créer le premier compte](#4-créer-le-premier-compte)
5. [Les API](#5-les-api)
6. [Rappel Git](#6-rappel-git)
7. [Captures d'écran](#7-captures-décran)
8. [Tests des règles de gestion](#8-tests-des-règles-de-gestion-rg-01-à-rg-06)

---

## 1. Prérequis

| Outil | Version | Vérifier avec |
|---|---|---|
| JDK | 17 ou + | `java -version` |
| Node.js | 20 ou + | `node -v` |
| npm | fourni avec Node | `npm -v` |
| Docker Desktop | à jour, **démarré** | `docker -v` puis `docker compose version` |
| Git | quelconque | `git --version` |
| IDE | IntelliJ IDEA / VS Code | — |

---

## 2. Arborescence

```
bibliothèque/
├── bibliotheque-backend/           API REST Spring Boot — port 8080
│   ├── pom.xml                     dépendances Maven + version de Java
│   ├── mvnw, mvnw.cmd              wrapper Maven (pas besoin d'installer Maven)
│   └── src/
│       ├── main/java/com/ibizabroker/bibliotheque/
│       │   ├── BibliothequeApplication.java   point d'entrée (main)
│       │   ├── entity/             les objets métier == les tables
│       │   │   ├── Books.java          un livre (+ borrowBook / returnBook)
│       │   │   ├── Users.java          un utilisateur, lié à des Role
│       │   │   ├── Role.java           "Admin" ou "User"
│       │   │   ├── Borrow.java         un emprunt (dates emprunt / retour)
│       │   │   ├── Reservation.java    une réservation (statut, dates)
│       │   │   ├── ReservationStatut.java  enum : EN_ATTENTE, DISPONIBLE, ANNULEE, EXPIREE, HONOREE
│       │   │   ├── JwtRequest.java     corps du POST /authenticate
│       │   │   ├── JwtResponse.java    réponse : utilisateur + token
│       │   │   └── JsonDataSerializer.java  formate les dates en dd-MM-yyyy
│       │   ├── dao/                accès base — Spring Data JPA
│       │   │   ├── BooksRepository.java
│       │   │   ├── UsersRepository.java     findByUsername
│       │   │   ├── BorrowRepository.java    findByUserId, findByBookId
│       │   │   └── ReservationRepository.java
│       │   ├── controller/         les points d'entrée HTTP
│       │   │   ├── BooksController.java     /admin/books
│       │   │   ├── AdminController.java     /admin/users
│       │   │   ├── BorrowController.java    /borrow
│       │   │   ├── JwtController.java       /authenticate
│       │   │   └── ReservationController.java  /api/reservations
│       │   ├── service/
│       │   │   ├── JwtService.java     vérifie le couple login / mot de passe
│       │   │   └── ReservationService.java  logique métier réservation (RG-01 à RG-06)
│       │   ├── dto/
│       │   │   ├── ReservationRequestDTO.java
│       │   │   └── ReservationResponseDTO.java
│       │   ├── exceptions/
│       │   │   ├── NotFoundException.java     -> HTTP 404
│       │   │   ├── ConflictException.java    -> HTTP 409
│       │   │   └── BadRequestException.java  -> HTTP 400
│       │   ├── configuration/
│       │   │   ├── WebSecurityConfiguration.java     qui a le droit d'aller où
│       │   │   ├── JwtRequestFilter.java             lit le header Authorization
│       │   │   ├── JwtAuthenticationEntryPoint.java  renvoie 401
│       │   │   └── CorsConfiguration.java            autorise le front
│       │   └── util/JwtUtil.java       fabrique et valide les tokens
│       ├── main/resources/application.properties     port, URL base, identifiants
│       └── test/java/...           tests unitaires
│
├── bibliotheque-frontend/          interface Angular — port 4200
│   ├── package.json                dépendances npm + scripts
│   ├── angular.json                configuration de build
│   └── src/
│       ├── index.html              la seule vraie page HTML
│       ├── main.ts                 démarre AppModule
│       └── app/
│           ├── app.module.ts       déclare composants, services, intercepteur
│           ├── app-routing.module.ts   URL -> composant, + rôles autorisés
│           ├── _model/             les types TypeScript (books, users, borrow)
│           ├── _service/           les appels HTTP vers le backend
│           │   ├── books.service.ts      CRUD livres
│           │   ├── users.service.ts      CRUD utilisateurs + login
│           │   ├── borrow.service.ts     emprunts
│           │   └── user-auth.service.ts  token + rôles dans localStorage
│           ├── _auth/
│           │   ├── auth.guard.ts         bloque une route selon le rôle
│           │   └── auth.interceptor.ts   ajoute "Bearer <token>" partout
│           └── <15 composants>/    un dossier par écran (html / css / ts / spec)
│
├── docker/
│   └── init/
│       └── 01-create-db.sql        crée la base au premier démarrage Docker
├── docker-compose.yml              PostgreSQL 16 (port 5432)
└── screenshots/                    captures utilisées plus bas
```

**La règle à retenir** : côté backend, un dossier = une responsabilité
(`controller` reçoit, `service` décide, `dao` persiste, `entity` représente).
Côté frontend, un dossier = un écran, et tout ce qui parle au réseau vit dans
`_service`.

---

## 3. Démarrer le projet

### 3.1 La base de données

Lancez PostgreSQL avec Docker :

```bash
docker compose up -d
```

Le schéma `KAFOKAMLybrery48` est créé automatiquement par
`docker/init/01-create-db.sql`.

Les identifiants attendus sont dans
`bibliotheque-backend/src/main/resources/application.properties` :
utilisateur `postgres`, mot de passe `postgres`, port `5432`. Adaptez le fichier
à votre installation **ou** votre installation au fichier — mais sachez lequel
des deux vous avez fait.

> Si vous avez PostgreSQL installé en local, vous pouvez aussi créer la base
> manuellement et lancer le backend sans Docker.

### 3.2 Le backend

```bash
cd bibliotheque-backend
./mvnw spring-boot:run          # Windows : mvnw.cmd spring-boot:run
```

Au démarrage, `spring.jpa.hibernate.ddl-auto=update` demande à Hibernate de
créer les tables manquantes. Vérifiez-le tout de suite :

```sql
USE "KAFOKAMLybrery48";
SHOW TABLES;
```

L'API écoute sur **http://localhost:8080**.

### 3.3 Le frontend

```bash
cd bibliotheque-frontend
npm install
npm start                       # équivaut à : ng serve
```

L'interface est sur **http://localhost:4200**. Elle appelle le backend sur le
port 8080 : les deux doivent tourner en même temps.

### 3.4 Vérification

1. Le backend affiche `Started BibliothequeApplication` dans sa console.
2. Ouvrir **http://localhost:4200** : la page d'accueil / de connexion s'affiche.
3. Se connecter avec `admin` / `admin123` (voir section suivante).

---

## 4. Créer le premier compte

Il n'y a aucun utilisateur en base, et `POST /admin/users` exige déjà un token.
Le premier administrateur s'insère donc directement en SQL, **après** le premier
démarrage du backend — sinon les tables n'existent pas encore.

Le mot de passe doit être un hachage **BCrypt** : `WebSecurityConfiguration`
déclare un `BCryptPasswordEncoder`, il n'acceptera jamais un mot de passe en
clair. Le hachage ci-dessous correspond à `admin123`.

```sql
-- 1. Regardez d'abord ce qu'Hibernate a réellement créé.
SHOW TABLES;
DESCRIBE users;
DESCRIBE role;

-- 2. Puis insérez, en adaptant aux colonnes que DESCRIBE vous a montrées.
INSERT INTO role (role_name) VALUES ('Admin'), ('User');

INSERT INTO users (user_id, username, name, password)
VALUES (1, 'admin', 'Administrateur',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696');

INSERT INTO user_role (user_id, role_id)
VALUES (1, (SELECT role_id FROM role WHERE role_name = 'Admin'));

-- 3. Si une table hibernate_sequence existe, avancez son compteur au-delà
--    des identifiants que vous venez de poser à la main.
UPDATE hibernate_sequence SET next_val = 100 WHERE next_val < 100;
```

Connexion : **admin / admin123**.

Vérification en ligne de commande :

```bash
curl -X POST http://localhost:8080/authenticate \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
```

Vous devez recevoir un JSON contenant `jwtToken`. Gardez-le : il sert pour tous
les autres appels.

```bash
curl http://localhost:8080/admin/users -H "Authorization: Bearer <le_token>"
```

---

## 5. Les API

Base : `http://localhost:8080`

### Authentification

`POST /authenticate` — accessible sans token, renvoie l'utilisateur et son JWT.

```json
{ "username": "admin", "password": "admin123" }
```

### Livres — `/admin/books`

| Verbe | URL | Rôle | Description |
|---|---|---|---|
| GET | `/admin/books` | — | Liste tous les livres |
| GET | `/admin/books/{id}` | Admin | Un livre par son id |
| POST | `/admin/books` | Admin | Crée un livre |
| PUT | `/admin/books/{id}` | Admin | Modifie un livre |
| DELETE | `/admin/books/{id}` | Admin | Supprime un livre |

```json
{
    "bookName": "Le Petit Prince",
    "bookAuthor": "Antoine de Saint-Exupéry",
    "bookGenre": "Conte",
    "noOfCopies": 5
}
```

### Utilisateurs — `/admin/users`

| Verbe | URL | Rôle | Description |
|---|---|---|---|
| GET | `/admin/users` | Admin | Liste les utilisateurs |
| GET | `/admin/users/{id}` | Admin | Un utilisateur par son id |
| POST | `/admin/users` | authentifié | Crée un utilisateur (le mot de passe est chiffré ici) |
| PUT | `/admin/users/{id}` | Admin | Modifie un utilisateur |

```json
{
    "username": "marie",
    "name": "Marie Dupont",
    "password": "motdepasse",
    "role": [ { "roleName": "User" } ]
}
```

### Emprunts — `/borrow`

| Verbe | URL | Description |
|---|---|---|
| GET | `/borrow` | Tous les emprunts |
| GET | `/borrow/user/{id}` | Les emprunts d'un utilisateur |
| GET | `/borrow/book/{id}` | L'historique d'un livre |
| POST | `/borrow` | Emprunter : décrémente `noOfCopies`, échéance à 7 jours |
| PUT | `/borrow` | Rendre : incrémente `noOfCopies`, pose la date de retour |

```json
{ "bookId": 3, "userId": 5 }
```

### Réservations — `/api/reservations`

| Verbe | URL | Description |
|---|---|---|
| POST | `/api/reservations` | Créer une réservation (livre doit être indisponible) |
| GET | `/api/reservations` | Lister les réservations (filtrable par `statut` et `adherentId`) |
| GET | `/api/reservations/expirees` | Lister les réservations expirées |
| GET | `/api/reservations/{id}` | Consulter une réservation |
| PATCH | `/api/reservations/{id}/annuler` | Annuler une réservation |
| DELETE | `/api/reservations/{id}` | Supprimer une réservation |

**Règles métier (RG-01 à RG-06) :**

| RG | Règle |
|---|---|
| RG-01 | On ne peut réserver qu'un livre indisponible (`noOfCopies == 0`) |
| RG-02 | Un adhérent ne peut avoir qu'une seule réservation active sur un même livre |
| RG-03 | Un adhérent ne peut pas dépasser 3 réservations actives simultanées |
| RG-04 | Les réservations expirent après 7 jours |
| RG-05 | L'annulation est possible seulement si le statut est `EN_ATTENTE` ou `DISPONIBLE` |
| RG-06 | Les statuts terminaux (`ANNULEE`, `EXPIREE`, `HONOREE`) ne peuvent plus changer |

**Flux des statuts :**
`EN_ATTENTE` → `DISPONIBLE` → `HONOREE` | `ANNULEE` | `EXPIREE`

```json
{ "livreId": 3, "adherentId": 5 }
```

---

## 6. Rappel Git

Le cycle complet, dans l'ordre :

```bash
# 1. Partir d'une base à jour
git checkout main
git pull

# 2. Une branche par sujet. Nommez-la pour qu'on devine son contenu.
git switch -c feat/nom-du-sujet

# 3. Travailler, puis regarder ce qu'on s'apprête à livrer
git status
git diff

# 4. Choisir ce qui entre dans le commit — pas de "git add ." aveugle
git add <fichiers>
git commit -m "feat: message à l'impératif, une ligne, ce qui change et pourquoi"

# 5. Publier la branche
git push -u origin feat/nom-du-sujet

# 6. Ouvrir la Pull Request sur GitHub, et y décrire :
#    ce que ça fait, comment le tester, ce qui reste à faire.
```

Quelques réflexes :

* `git log --oneline --graph --all` pour voir où vous en êtes.
* Un commit = un changement cohérent. Dix fichiers sans rapport dans un commit,
  c'est une revue impossible.
* On ne pousse jamais sur `main` directement.
* `node_modules/`, `target/` et `dist/` ne sont **jamais** commités : c'est le
  rôle des `.gitignore` du dépôt. Si `git status` vous les propose, quelque
  chose ne va pas.

---

## 7. Captures d'écran

### Accueil et connexion

![Page d'accueil](./screenshots/home.png "Page d'accueil")
![Page de connexion](./screenshots/login.png "Page de connexion")

### Côté administrateur

| Écran | Aperçu |
|---|---|
| Liste des livres | ![Liste des livres](./screenshots/book_list.png) |
| Ajout d'un livre | ![Ajout d'un livre](./screenshots/book_add.png) |
| Modification | ![Modification](./screenshots/book_update.png) |
| Historique d'un livre | ![Détail livre](./screenshots/book_details.png) |
| Liste des utilisateurs | ![Liste des utilisateurs](./screenshots/user_list.png) |
| Ajout d'un utilisateur | ![Ajout utilisateur](./screenshots/user_add.png) |
| Emprunts d'un utilisateur | ![Détail utilisateur](./screenshots/user_details.png) |

### Côté utilisateur

| Écran | Aperçu |
|---|---|
| Emprunter | ![Emprunter](./screenshots/borrow_book.png) |
| Rendre | ![Rendre](./screenshots/return_book.png) |
| Accès refusé | ![Forbidden](./screenshots/forbidden.png) |

---

## 8. Tests des règles de gestion (RG-01 à RG-06)

Tests réalisés via Swagger UI (`http://localhost:8080/swagger-ui.html`).
Chaque test est documenté avec la requête, le résultat attendu et la capture.

### Données de test

| Réf. | Type | ID | Détails |
|---|---|---|---|
| L1 | Livre disponible | bookId: 107 | `noOfCopies: 3` |
| L2 | Livre emprunté | bookId: 108 | `noOfCopies: 0` |
| L3 | Livre emprunté | bookId: 109 | `noOfCopies: 0` |
| L4 | Livre emprunté | bookId: 110 | `noOfCopies: 0` |
| L5 | Livre emprunté | bookId: 111 | `noOfCopies: 0` |
| A1 | Réservataire principal | userId: 112 | username: `a1_reservataire` |
| A2 | Saturation quota | userId: 113 | username: `a2_quota` |
| A3 | Emprunteur L2-L5 | userId: 114 | username: `a3_emprunteur` |

### Préparation

1. Ouvrir Swagger : `http://localhost:8080/swagger-ui.html`
2. S'authentifier via `POST /authenticate` avec `admin` / `admin123`
3. Copier le `jwtToken` → cliquer **Authorize** → coller le token

### RG-01 — Réserver un livre disponible (interdit)

| | |
|---|---|
| **Requête** | `POST /api/reservations` |
| **Body** | `{ "livreId": 107, "adherentId": 112 }` |
| **Résultat** | **409** — `RG-01: reservation is only allowed for unavailable books.` |
| **Capture** | `screenshots/rg-01.png` |

### RG-02 — Deux réservations actives sur même livre (interdit)

| | |
|---|---|
| **1ère requête** | `POST /api/reservations` — `{ "livreId": 108, "adherentId": 112 }` → **201** (réservation créée, id retourné) |
| **2ème requête** | `POST /api/reservations` — `{ "livreId": 108, "adherentId": 112 }` |
| **Résultat** | **409** — `RG-02: an adherent can only have one active reservation per book.` |
| **Capture** | `screenshots/rg-02.png` |

### RG-03 — Saturer le quota de 3 réservations

| # | Body | Résultat |
|---|---|---|
| 1 | `{ "livreId": 109, "adherentId": 113 }` | 201 ✅ |
| 2 | `{ "livreId": 110, "adherentId": 113 }` | 201 ✅ |
| 3 | `{ "livreId": 111, "adherentId": 113 }` | 201 ✅ |
| 4 | `{ "livreId": 101, "adherentId": 113 }` | **409** ❌ |

| | |
|---|---|
| **Résultat (4ème)** | **409** — `RG-03: an adherent cannot exceed 3 active reservations.` |
| **Capture** | `screenshots/rg-03.png` |

### RG-04 — Vérifier dateExpiration = dateReservation + 7 jours

| | |
|---|---|
| **Requête** | `GET /api/reservations/{id}` (id de la réservation créée au test RG-02) |
| **Résultat** | **200** — `dateExpiration` = `dateReservation` + 7 jours |
| **Capture** | `screenshots/rg-04.png` |

### RG-05 — Annuler une réservation EN_ATTENTE

| | |
|---|---|
| **Requête** | `PATCH /api/reservations/{id}/annuler` (même id que RG-02) |
| **Résultat** | **200** — `statut: "ANNULEE"` |
| **Capture** | `screenshots/rg-05.png` |

### RG-06 — Annuler une réservation déjà ANNULEE (interdit)

| | |
|---|---|
| **Requête** | `PATCH /api/reservations/{id}/annuler` (même id) |
| **Résultat** | **409** — `RG-06: a reservation with status ANNULEE cannot be changed.` |
| **Capture** | `screenshots/rg-06.png` |
