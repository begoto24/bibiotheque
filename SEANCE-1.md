# Séance 1 — Remise en route et environnement

KFOKAM48 — Batch 2 — Phase 3 · Séances Full Stack

## Objectif

Repartir avec un environnement qui tourne, et savoir lancer de zéro un projet que vous n'avez pas écrit.

Aucune commande ne vous sera donnée. Le README du dépôt, la documentation officielle et vos recherches suffisent.
Chercher soi-même comment démarrer un projet inconnu fait partie de ce qui est évalué à l'épreuve.

## Le projet support

Gestion de Bibliothèque — backend Spring Boot, base PostgreSQL, frontend.

Dépôt : https://github.com/KFOKAM48/bibiotheque.git

Ce projet n'a aucun rapport avec Sekouh ni Super App. Personne ne part avec un avantage.

## À installer avant la séance

JDK 17+ · Node 20+ · Docker Desktop · Git · un IDE · Postman

Ne venez pas avec une machine vierge. Cette séance sert à faire tourner un projet, pas à télécharger des installateurs.

## Les tâches de la séance

1. **Cloner le projet et cartographier son arborescence**
   Où est le backend, le frontend, la configuration, les entités, les contrôleurs, les services ?

2. **Démarrer PostgreSQL dans un conteneur Docker**
   La base ne s'installe pas sur votre machine. À vous de trouver comment la lancer, et comment la configurer côté Spring Boot.

3. **Vous connecter à la base et lister les tables**

4. **Lancer le backend, puis le frontend**
   Lisez les logs de démarrage. Vous devez comprendre ce qu'ils vous disent.

5. **Explorer l'API dans Swagger, puis dans Postman**
   Récupérer la liste des livres, en créer un, observer les codes HTTP retournés.

6. **Suivre l'ajout d'un livre de bout en bout**
   Du clic jusqu'à la ligne dans la table PostgreSQL, en ouvrant chaque fichier traversé. C'est l'exercice central de la séance.

7. **Faire un cycle Git complet**
   Branche, commit, push, Pull Request, revue par un voisin, fusion. Sur le README, sans risque.

## Ce qui sera vérifié en fin de séance

* Votre application tourne sur votre machine
* Vous savez expliquer le trajet d'une donnée à travers les couches
* Votre Pull Request a été fusionnée

## Après la séance

Refaites tout seul, chez vous, sans notes.

Un environnement qui a démarré une fois avec l'aide du voisin ne prouve rien. Si vous n'y arrivez pas seul, signalez-le avant la séance suivante.
