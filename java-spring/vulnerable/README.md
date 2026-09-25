# TaskForge — version Java (Spring Boot) — À AUDITER

> Application volontairement construite pour l'exercice d'audit. Ne pas déployer.

## Prérequis (étape 0)

- JDK 17 installé (`java -version`)
- Maven 3.8+ installé (`mvn -version`) — ou utiliser le wrapper si présent
- Accès Internet pour le téléchargement des dépendances

## Démarrage

```bash
cd java-spring/vulnerable
mvn spring-boot:run
```

Application sur http://localhost:8080
Base H2 en mémoire, réinitialisée à chaque démarrage.

## Comptes de démonstration

| Identifiant | Mot de passe | Rôle  |
|-------------|--------------|-------|
| admin       | admin        | ADMIN |
| alice       | password1    | USER  |
| bob         | hunter2      | USER  |

## Exercice

Application à l'architecture soignée (contrôleurs, services, repositories Spring
Data, gabarits Thymeleaf) mais qui contient **une faille par catégorie de
l'OWASP Top 10:2025**. Les identifier par lecture du code et manipulation, puis
les cartographier sur la grille A01 → A10. Reporter les trouvailles sur
`FICHE-APPRENANT-v1.0.md`.
