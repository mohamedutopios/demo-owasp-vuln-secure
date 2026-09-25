# TaskForge — version Java (Spring Boot) — CONFORME

> Version corrigée, à comparer ligne à ligne avec `java-spring/vulnerable`.
> Chaque correctif est annoté en commentaire `// A0x` dans le code.

## Prérequis (étape 0)

- JDK 17 ou supérieur installé (`java -version`)
- Maven 3.8+ installé (`mvn -version`)
- Accès Internet pour le téléchargement des dépendances Maven

## Secrets (étape 0)

Les secrets ne sont pas dans le code. Des valeurs de secours permettent le
démarrage en lab ; **en production, définir ces variables et retirer les défauts**
de `application.properties` :

```bash
export APP_ENC_PASSWORD="une-phrase-secrete-robuste"
export APP_ENC_SALT="5c0744940b5c369b"   # sel hexadécimal
```

## Démarrage

```bash
cd java-spring/secure
mvn spring-boot:run
```

Application sur http://localhost:8080
Base H2 en mémoire, réinitialisée à chaque démarrage. Console H2 **désactivée**.

## Comptes de démonstration

| Identifiant | Mot de passe     | Rôle  |
|-------------|------------------|-------|
| admin       | Admin!Passw0rd   | ADMIN |
| alice       | Alice!Passw0rd   | USER  |
| bob         | Bob!Passw0rd42   | USER  |

Les mots de passe respectent la politique (≥12 caractères, majuscule, minuscule,
chiffre) et sont stockés hachés (BCrypt, coût 12).

## Ce qui change par rapport à la version vulnérable

| Catégorie | Correctif |
|-----------|-----------|
| A01 | Autorisation centralisée Spring Security ; contrôle d'accès objet (anti-IDOR) ; `@PreAuthorize` admin ; anti-SSRF (`UrlSafety`) |
| A02 | Config durcie (`application.properties`), en-têtes CSP/HSTS/nosniff/frame-deny, actuator restreint, H2 console off, pas de trace |
| A03 | Spring Boot 3.3.4, dépendances transitives à jour |
| A04 | BCrypt ; IBAN chiffré au repos (AES-GCM, clé dérivée d'un secret d'environnement) ; IBAN masqué à l'affichage |
| A05 | Recherche par requête dérivée paramétrée (fin du SQL natif concaténé) ; `th:text` (fin du XSS) |
| A06 | Rôle imposé à l'inscription (pas de mass assignment) ; politique de mot de passe ; réinitialisation non énumérable |
| A07 | Auth par framework, session recréée (anti-fixation), limitation des tentatives, logout POST |
| A08 | CSRF activé ; préférences en JSON signé HMAC (fin d'`ObjectInputStream`) |
| A09 | Journalisation des événements de sécurité sans mot de passe |
| A10 | Échecs sûrs (fail-closed), page d'erreur générique, aucune trace divulguée |
