# Épreuve — Séance 1 : Remise en route et environnement

**Nom / Prénom :** Djoukoya-De-Begoto Prince Malachie
**Branche :** `epreuve/djoukoya-de-begoto-prince-malachie`
**Date :** 14 août 2026

---

## Environnement (Partie 1 — 4 points)

### 1.1 — Versions de la machine (sortie brute)

```
$ java -version
openjdk version "21.0.9" 2025-10-21 LTS
OpenJDK Runtime Environment Temurin-21.0.9+10 (build 21.0.9+10-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.9+10 (build 21.0.9+10-LTS, mixed mode, sharing)

$ node -v
v22.23.2

$ npm -v
11.10.0

$ docker compose version
/usr/bin/bash: line 1: docker: command not found

$ git --version
git version 2.50.1.windows.1
```

> ⚠️ **Constat important** : `docker` n'est pas trouvé. Docker Desktop n'est pas
> opérationnel sur cette machine (dossier `C:\Program Files\Docker` présent mais
> incomplet, `docker` absent du PATH). **La partie 3 est bloquée par ce point** :
> l'installation de Docker Desktop est en cours.

### 1.2 — Le Java vu par Maven

```
$ ./mvnw -version
Apache Maven 3.8.5
Maven home: C:\Users\Begoto Prince\.m2\wrapper\dists\apache-maven-3.8.5-bin\...
Java version: 21.0.9, vendor: Eclipse Adoptium, runtime: C:\Users\Begoto Prince\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.9.10-hotspot
```

Oui, c'est **la même version** que `java -version` (21.0.9, Eclipse Adoptium).
Maven n'utilise pas le `java` du `PATH` mais celui pointé par la variable
d'environnement **`JAVA_HOME`**, qui est réglée sur
`C:\Users\Begoto Prince\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.9.10-hotspot`.
Ici `JAVA_HOME` et le `PATH` désignent le même JDK ; si deux JDK étaient
installés, ce sont ces deux endroits qui pourraient diverger.

### 1.3 — `./mvnw clean package`

Sur ma machine, le build **réussit** :

```
[INFO] Building jar: ...\bibliotheque-backend\target\bibliotheque-0.0.1-SNAPSHOT.jar
[INFO] --- spring-boot-maven-plugin:2.4.5:repackage (repackage) @ bibliotheque ---
[INFO] Replacing main artifact with repackaged archive
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:31 min
```

**Quel JDK j'utilise et pourquoi ça passe :** je compile avec le **JDK 21
(Temurin)**. Le projet cible à l'origine Spring Boot 2.4.5 et Java 1.8, et la
version de Lombok embarquée par défaut était trop ancienne pour le compilateur
du JDK 21 : au premier essai, le build s'arrêtait sur
`NoSuchFieldError: Class com.sun.tools.javac.tree.JCTree$JCImport does not have member field 'qualid'`.
Lombok accède aux *internals* du compilateur Java, dont la structure a changé
entre Java 8 et Java 21. La correction choisie a été de **pénaliser la version
de Lombok en `1.18.32`** dans `pom.xml`, compatible avec le JDK 21.

Les **deux stratégies** possibles pour ce type d'échec :

| Stratégie | Avantage | Inconvénient |
|---|---|---|
| **Moderniser** : monter la version de Lombok (choisie ici) | On garde un JDK récent et sécurisé | Toucher au `pom.xml`, rester sur un vieux Spring Boot 2.4.5 |
| **Construire avec un JDK plus ancien** (8/11) | Aucun changement de code | JDK 8/11 en fin de vie, et il faut un second JDK installé (ou une image Docker dédiée) |

---

## Arborescence (Partie 2 — 4 points)

### 2.1 — URL, utilisateur et mot de passe de la base de données

`bibliotheque-backend/src/main/resources/application.properties` :
`spring.datasource.url=jdbc:postgresql://localhost:5432/KAFOKAMLybrery48`,
`spring.datasource.username=postgres`, `spring.datasource.password=postgres`.

### 2.2 — Qui autorise `POST /authenticate` sans connexion

`bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java` :
la ligne `.antMatchers("/authenticate", "/borrow/**", "/admin/books/").permitAll()`.

### 2.3 — L'objet `Books` devient une ligne de table

`bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Books.java`.
La classe porte `@Entity @Table(name = "Books")` : elle vise la table **`Books`**.
(En PostgreSQL, le nom non quoté est replié en minuscules → la table créée par
Hibernate s'appelle `books`.)

### 2.4 — Qui écrit le code de `save()` ?

`bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/BooksRepository.java`
est une interface vide qui étend `JpaRepository<Books, Integer>`. C'est
**Spring Data JPA** qui génère l'implémentation (classe `SimpleJpaRepository`)
**au démarrage de l'application**, par introspection de l'interface.

### 2.5 — `http://localhost:8080` en dur — 4 occurrences dans 3 fichiers

1. `bibliotheque-frontend/src/app/_service/users.service.ts` — ligne 13
   (`baseURL = "http://localhost:8080/admin/users"`) et ligne 24
   (`http://localhost:8080/authenticate`)
2. `bibliotheque-frontend/src/app/_service/books.service.ts` — ligne 11
   (`baseURL = "http://localhost:8080/admin/books"`)
3. `bibliotheque-frontend/src/app/_service/borrow.service.ts` — ligne 11
   (`baseURL = "http://localhost:8080/borrow"`)

C'est un problème dès qu'on déploie ailleurs : l'adresse est celle de la machine
de développement ; sur un autre poste ou un serveur, rien ne répond sur
`localhost:8080` (c'est *votre* machine qui est visée, pas l'utilisateur).
Il faudrait passer par `src/environments/environment.ts`.

### 2.6 — Routes protégées par rôle

Fichier : `bibliotheque-frontend/src/app/app-routing.module.ts`.

- **Rôle `Admin`** : `books`, `create-book`, `update-book/:bookId`,
  `book-details/:bookId`, `users`, `register-user`, `user-details/:userId`,
  `update-user/:userId`.
- **Rôle `User`** : `borrow-book`, `return-book`.
- **Sans `canActivate`** : `''` (HomeComponent), `login`, `forbidden`.
  C'est normal pour les trois : la page d'accueil doit être publique, la page de
  connexion doit être accessible sans authentification, et la page *forbidden*
  doit être joignable pour afficher le refus d'accès.

---

## Trajet d'une donnée (Partie 4 — 4 points)

### 4.1 — L'emprunt d'un livre, du clic à la base

| # | Couche | Fichier (chemin exact) | Ce qui s'y passe |
|---|---|---|---|
| 1 | Composant | `bibliotheque-frontend/src/app/borrow-book/borrow-book.component.ts` | Le clic sur le bouton appelle le service d'emprunt avec `{ bookId, userId }` |
| 2 | Service | `bibliotheque-frontend/src/app/_service/borrow.service.ts` | Traduit l'appel en `POST http://localhost:8080/borrow` (baseURL ligne 11) |
| 3 | Intercepteur | `bibliotheque-frontend/src/app/_auth/auth.interceptor.ts` | Ajoute `Authorization: Bearer <token>` à la requête |
| 4 | CORS | `bibliotheque-backend/.../configuration/CorsConfiguration.java` | Autorise l'origine du front (4200) |
| 5 | Filtre JWT | `bibliotheque-backend/.../configuration/JwtRequestFilter.java` | Valide le token et pose l'utilisateur dans le `SecurityContext` |
| 6 | Sécurité | `bibliotheque-backend/.../configuration/WebSecurityConfiguration.java` | `/borrow/**` est en `permitAll` — **aucun contrôle de rôle côté serveur** sur l'emprunt |
| 7 | Contrôleur | `bibliotheque-backend/.../controller/BorrowController.java` — `borrowBook()` | Charge l'utilisateur et le livre, vérifie le stock, décrémente `noOfCopies`, pose `issueDate` (maintenant) et `dueDate` (J+7), sauvegarde l'emprunt |
| 8 | Repositories | `dao/BorrowRepository.java`, `dao/BooksRepository.java`, `dao/UsersRepository.java` | `save()` / `findById()` générés par Spring Data JPA |
| 9 | Entités | `entity/Borrow.java` (`@Table(name = "Borrow")`), `entity/Books.java` | Décrivent les tables et les colonnes |
| 10 | Hibernate | logs du backend (`show-sql=true`) | Émet un `UPDATE` sur `books` **et** un `INSERT` sur `borrow` |
| 11 | Base | PostgreSQL | Deux lignes touchées : `books.no_of_copies` décrémenté + nouvelle ligne dans `borrow` |

**Ce que `borrowBook()` fait de particulier :** la création d'un livre ne touche
qu'**une** table (`books`). L'emprunt en touche **deux** : `books` (le compteur
`no_of_copies` est décrémenté, `book.borrowBook()` puis `booksRepository.save()`)
**et** `borrow` (nouvelle ligne avec les dates d'emprunt et d'échéance à J+7).

### 4.2 — Trois manipulations

| Manipulation | Code HTTP / comportement | Fichier responsable |
|---|---|---|
| `curl -X POST http://localhost:8080/admin/books` **sans** `Authorization` | **401 Unauthorized** | `configuration/JwtAuthenticationEntryPoint.java` (renvoie 401 via `sendError`) |
| Se connecter en rôle `User`, puis ouvrir `/books` dans le navigateur | **Aucun appel réseau** : redirection vers `/forbidden` | `bibliotheque-frontend/src/app/_auth/auth.guard.ts` (bloque côté navigateur avant toute requête ; `@PreAuthorize("hasRole('Admin')")` dans `BooksController` ne serait atteint que si la requête partait) |
| `GET /admin/books/9999` avec un token admin valide | **404 Not Found** (livre inexistant) | `controller/BooksController.java` → `exceptions/NotFoundException.java` (annotée `@ResponseStatus(HttpStatus.NOT_FOUND)`) |

La manipulation qui ne produit **aucun appel réseau** est la deuxième : le
`AuthGuard` vérifie le rôle **dans le navigateur** et redirige avant que la
requête HTTP ne soit émise.

### 4.3 — La double protection est-elle redondante ?

Non. Le `auth.guard.ts` protège l'**interface** : il évite des requêtes inutiles
et améliore l'expérience, mais il s'exécute chez le client et peut être
contourné (on peut appeler l'API directement avec `curl`). Le `@PreAuthorize`
est la vraie sécurité, côté **serveur**, la seule source de vérité. S'il fallait
n'en garder qu'une, ce serait la protection serveur — celle du navigateur n'est
que cosmétique.

---

## Partie 3 — Docker

❌ **Non réalisée — repli assumé (diagnostic honnête).**

Docker n'est pas fonctionnel sur ma machine : `docker compose version` répond
`docker: command not found` (installation incomplète). Les points de cette
partie (6 pts) ne sont donc **pas collectés**, et les captures
`docker-compose-ps.png` / `application.png` ne peuvent pas être produites.

Conformément au repli prévu par la séance (*« Repli si vous bloquez : lancez
les trois à la main... notez que le repli n'est pas le rendu attendu »*),
l'application tourne sans Docker : **PostgreSQL local** (pgAdmin/psql) +
`./mvnw spring-boot:run` (port 8080) + `npm start` (port 4200).

> **Écart assumé avec l'énoncé** : l'épreuve a été écrite pour le projet
> d'origine en MySQL. Le dépôt ayant été migré en **PostgreSQL** avant la
> séance (pilote `postgresql` dans `pom.xml`, `application.properties` en
> PostgreSQL), une éventuelle partie 3 utiliserait un conteneur **PostgreSQL**,
> pas MySQL — c'est le code qui fait foi.

## Bonus — Un vrai défaut documenté

**Défaut : le hash BCrypt des mots de passe est renvoyé par l'API.**

- **Où** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/AdminController.java`
  renvoie les entités `Users` telles quelles ; vérifié en testant
  `GET /admin/users` avec un token admin : la réponse JSON contient
  `"password":"$2b$10$RN5ij..."` pour chaque utilisateur.
- **Risque** : un hash BCrypt reste attaquable (dictionnaire / force brute), et
  le stockage d'un hash n'a de sens que s'il ne sort jamais de la base. Tout
  utilisateur admin, ou toute fuite d'une réponse, expose les hashes.
- **Correction proposée** : ne jamais sérialiser le champ `password`
  (annotation `@JsonIgnore` sur `Users.password`, ou DTO sans mot de passe).

*(Deux autres défauts repérés, non développés ici : les réponses d'erreur
exposent la stack trace complète — `server.error.include-stacktrace` — et
`POST /borrow` est accessible sans authentification alors qu'il modifie le
stock.)*
