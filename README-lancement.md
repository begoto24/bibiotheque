# Bibliothèque — Guide de lancement

Guide personnel pour lancer le projet **Bibliothèque** : backend Spring Boot + base
PostgreSQL + frontend Angular.

## Prérequis

| Outil | Version |
|---|---|
| JDK | 17 ou supérieur |
| Node.js | 20 ou supérieur |
| npm | fourni avec Node |
| PostgreSQL | installé (psql / pgAdmin) |

## 1. Créer la base de données

Le backend ne crée pas la base, seulement les tables. Créer la base **avant** le
premier lancement :

```bash
psql -U postgres
```

```sql
CREATE DATABASE "KAFOKAMLybrery48";
```

> Les guillemets doubles sont obligatoires : le nom contient des majuscules.

La connexion est configurée dans
`bibliotheque-backend/src/main/resources/application.properties`
(URL `jdbc:postgresql://localhost:5432/KAFOKAMLybrery48`, utilisateur `postgres`,
mot de passe `postgres`, port 5432). Adapter ce fichier si votre installation diffère.

## 2. Lancer le backend (port 8080)

```bash
cd bibliotheque-backend
./mvnw spring-boot:run
```

- **Git Bash** : le préfixe `./` est obligatoire (`mvnw` seul → `command not found`).
- **Windows PowerShell** : `mvnw.cmd spring-boot:run`.
- Au démarrage, Hibernate crée les tables manquantes (`ddl-auto=update`).
- Attendre le message `Started BibliothequeApplication` dans les logs.
- L'API écoute sur **http://localhost:8080**.

## 3. Lancer le frontend (port 4200)

```bash
cd bibliotheque-frontend
npm install
npm start
```

- L'interface est sur **http://localhost:4200**.
- Elle appelle le backend sur le port 8080 : **les deux doivent tourner en même temps**
  (le CORS entre 4200 et 8080 est déjà configuré côté backend).

## 4. Vérification

1. Le backend affiche `Started BibliothequeApplication` dans sa console.
2. Ouvrir **http://localhost:4200** : la page d'accueil / de connexion s'affiche.

## Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `mvnw: command not found` | Exécutable non préfixé | Utiliser `./mvnw` (Git Bash) |
| `java: command not found` dans Git Bash | `PATH` différent entre terminaux | Vérifier `JAVA_HOME` (le wrapper Maven s'en sert) |
| `NoSuchFieldError ... JCTree$JCImport` | JDK trop récent pour le projet | Lombok 1.18.32 doit être déclaré dans `pom.xml` (déjà fait) |
| `Connection refused` vers la base | PostgreSQL arrêté, base absente ou mauvais identifiants | Vérifier PostgreSQL, créer la base, corriger `application.properties` |
| `Unrecognised tag: 'dependency'` | Balise XML mal fermée / dupliquée dans `pom.xml` | Réparer le XML |
| `authentification par mot de passe échouée` | Mauvais mot de passe dans `application.properties` | Mettre le vrai mot de passe PostgreSQL |

## Rappel

Il n'y a aucun compte en base au premier démarrage, et la création d'un utilisateur via
l'API exige déjà un token. Pour créer le premier administrateur, suivre la section 5 du
README principal du dépôt (insertion SQL d'un hachage BCrypt, connexion `admin` /
`admin123`).
