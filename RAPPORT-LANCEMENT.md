# Rapport de lancement — Projet Bibliothèque

**Séance 1 — Remise en route et environnement**
**Batch 2 — Phase 3 · Séances Full Stack**
**Projet :** Gestion de Bibliothèque (Spring Boot + PostgreSQL + Angular)
**Dépôt :** https://github.com/KFOKAM48/bibiotheque.git

---

## 1. Contexte

Objectif de la séance : repartir de zéro sur un projet que je n'ai pas écrit — cloner,
cartographier l'arborescence, démarrer une base PostgreSQL, lancer le backend puis le
frontend, et comprendre le trajet d'une donnée à travers les couches. Aucune commande
n'était fournie : le README du dépôt et la documentation officielle étaient les seules
sources autorisées.

## 2. Environnement de départ

| Outil | État constaté |
|---|---|
| JDK | 21 (trop récent pour le projet à l'origine) |
| Node.js | 20+ |
| PostgreSQL | déjà installé sur la machine (pgAdmin + psql) |
| Terminal | Git Bash |
| Docker Desktop | non utilisé pour la base (PostgreSQL local disponible) |

## 3. Déroulement

### 3.1 Clonage et cartographie

```bash
git clone https://github.com/KFOKAM48/bibiotheque.git
```

Arborescence identifiée :
- **Backend** : `bibliotheque-backend/` (Spring Boot) — configuration (`pom.xml`,
  `application.properties`), entités (`entity/`), contrôleurs (`controller/`),
  services (`service/`).
- **Frontend** : `bibliotheque-frontend/` (Angular).

### 3.2 Démarrage de PostgreSQL et création de la base

Le backend ne crée **pas** la base, seulement les tables. La base devant s'appeler
`KAFOKAMLybrery48`, je l'ai créée en ligne de commande :

```sql
CREATE DATABASE "KAFOKAMLybrery48";
```

> Les guillemets doubles sont **obligatoires** : le nom contient des majuscules et
> PostgreSQL les convertit en minuscules sans eux.

### 3.3 Lancement du backend

```bash
cd bibliotheque-backend
./mvnw spring-boot:run
```

Au démarrage, Hibernate crée automatiquement les tables manquantes
(`spring.jpa.hibernate.ddl-auto=update`) : `books`, `borrow`, `role`, `users`, etc.
L'API écoute sur **http://localhost:8080**.

### 3.4 Lancement du frontend

```bash
cd bibliotheque-frontend
npm install
npm start
```

L'interface est sur **http://localhost:4200** et appelle le backend sur le port 8080.
La configuration CORS entre les deux (4200 ↔ 8080) est déjà en place et cohérente.

## 4. Difficultés rencontrées et solutions

| # | Difficulté | Cause | Solution |
|---|---|---|---|
| 1 | `mvnw.cmd: command not found` | Sous Git Bash, il faut préfixer les exécutables du dossier courant par `./` | Utiliser `./mvnw` au lieu de `mvnw` |
| 2 | `java: command not found` dans Git Bash alors que Java marche en PowerShell | `PATH` différent entre les terminaux (variables d'environnement chargées différemment) | Non bloquant : le wrapper Maven trouve Java via `JAVA_HOME` |
| 3 | `NoSuchFieldError: Class com.sun.tools.javac.tree.JCTree$JCImport does not have member field 'qualid'` | Lombok par défaut de Spring Boot 2.4.5 trop ancien pour le JDK 21 (accès aux internals du compilateur, modifiés depuis Java 8) | Déclarer explicitement Lombok **1.18.32** dans le `pom.xml` |
| 4 | `Connection refused` sur `localhost:3306` | Projet configuré pour MySQL, mais MySQL absent — seul PostgreSQL est disponible | Migrer vers PostgreSQL : remplacer `mysql-connector-java` par `org.postgresql:postgresql` dans le `pom.xml` |
| 5 | `Unrecognised tag: 'dependency'` — POM non parsable | Balise `<dependency>` orpheline (dupliquée, sans fermeture) laissée lors de l'édition manuelle | Supprimer la balise en trop |
| 6 | Connexion impossible : la base n'existe pas | La base `KAFOKAMLybrery48` doit être créée à la main | `CREATE DATABASE "KAFOKAMLybrery48";` via `psql` |
| 7 | `FATAL: authentification par mot de passe échouée pour l'utilisateur « postgres »` | `application.properties` contenait encore un texte placeholder ou un mauvais mot de passe | Mettre le vrai mot de passe PostgreSQL (`postgres`) |

Adaptation du `application.properties` après migration PostgreSQL :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/KAFOKAMLybrery48
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 5. Résultat final

- ✅ Le backend démarre correctement : `Started BibliothequeApplication in 5.388 seconds`
- ✅ Hibernate crée automatiquement les tables (`books`, `borrow`, `role`, `users`, …)
- ✅ CORS entre le frontend (4200) et le backend (8080) déjà en place
- ✅ Frontend servie sur http://localhost:4200

**Fil conducteur des problèmes :** un environnement de développement partiellement
configuré (Java trop récent pour le projet, mauvaise base de données installée) combiné
à des éditions manuelles de fichiers de configuration qui ont introduit de petites
erreurs (XML mal fermé, placeholder oublié) — des soucis classiques quand on reprend un
vieux projet Spring Boot sur une machine neuve.

## 6. Récapitulatif des commandes

```bash
# 1. Créer la base (une seule fois)
psql -U postgres
CREATE DATABASE "KAFOKAMLybrery48";

# 2. Backend (port 8080)
cd bibliotheque-backend
./mvnw spring-boot:run

# 3. Frontend (port 4200)
cd bibliotheque-frontend
npm install
npm start
```

## 7. Prochaines étapes

- Créer le premier compte administrateur (aucun utilisateur n'existe en base ;
  `POST /admin/users` exige déjà un token — insertion SQL d'un hachage BCrypt,
  voir README principal du dépôt, section 5).
- Explorer l'API (récupérer la liste des livres, en créer un, observer les codes HTTP).
- Suivre l'ajout d'un livre de bout en bout (clic → requête → contrôleur → table).
- Faire le cycle Git complet (branche, commit, push, Pull Request).
