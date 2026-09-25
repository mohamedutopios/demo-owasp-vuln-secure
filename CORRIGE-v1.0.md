# TaskForge — Corrigé formateur (v1.0)

> Document réservé au formateur. Références de lignes valables pour la v1.0 du lab.
> Pour chaque catégorie : la faille (Python puis Java), sa démonstration, et le
> correctif correspondant dans la version `secure/`.

---

## A01 — Broken Access Control

Trois manifestations : IDOR/BOLA, contrôle de rôle manquant, et SSRF.

**IDOR (accès à l'objet d'autrui)**
- Python vuln : `python/vulnerable/app/tasks.py:49-52` — `detail()` charge la tâche
  par `id` sans vérifier le propriétaire.
- Java vuln : `java-spring/vulnerable/.../web/TaskController.java:72-73` — idem via
  `findById(id)`.
- Démo : se connecter en `alice`, ouvrir `/tasks/3` (tâche de `admin`/`bob`).
- Correctif Python : `python/secure/app/tasks.py` → `_owned_task_or_403()` (403 si
  ni propriétaire ni admin), appelé par `detail`/`comment`/`preview`.
- Correctif Java : `TaskController.java` → `ownedTaskOr403()` (jette 403/404).

**Contrôle de rôle manquant (BFLA)**
- Python vuln : `python/vulnerable/app/admin.py:9-13` — `@login_required` seulement,
  pas de contrôle ADMIN : un `USER` accède à `/admin/users`.
- Java vuln : `java-spring/vulnerable/.../web/AdminController.java:19-28` — aucun
  contrôle de rôle.
- Démo : se connecter en `alice`, ouvrir `/admin/users`.
- Correctif Python : décorateur `admin_required` (`app/auth.py`) sur les routes admin.
- Correctif Java : `SecurityConfig` (`/admin/** hasRole('ADMIN')`) + `@PreAuthorize`
  sur `AdminController`.

**SSRF**
- Python vuln : `python/vulnerable/app/tasks.py:71-75` — `requests.get(target)` sur
  une URL fournie par l'utilisateur, sans validation.
- Java vuln : `TaskController.java:90-94` — `new RestTemplate().getForObject(url,...)`.
- Démo : POST sur `/tasks/<id>/preview` avec `url=http://169.254.169.254/…` ou
  `http://localhost:…`.
- Correctif Python : `security.py` → `url_is_safe()` (schéma http/https, résolution
  DNS, blocage des plages privées/loopback/link-local).
- Correctif Java : `security/UrlSafety.java` (+ `HttpClient` sans suivi de redirection).

---

## A02 — Security Misconfiguration

- Python vuln :
  - `python/vulnerable/run.py:7` — `debug=True`, `host=0.0.0.0`.
  - `python/vulnerable/app/__init__.py:23-25` — CORS `*`, aucun en-tête de sécurité.
- Java vuln :
  - `application.properties:7,11,16,17` — `show-sql=true`, `include-stacktrace=always`,
    console H2 activée et ouverte, tous les endpoints actuator exposés.
  - `config/WebConfig.java:20` — CORS `allowedOrigins("*")`.
- Démo : provoquer une erreur (trace complète), ouvrir `/h2-console`, `/actuator`.
- Correctif Python : `app/__init__.py` `secure_headers()` (CSP, HSTS, nosniff,
  X-Frame-Options), pas de CORS ouvert, `debug=False` en production.
- Correctif Java : `application.properties` durci (`include-stacktrace=never`,
  console H2 off, actuator = `health`) + `SecurityConfig` `.headers(...)`.

---

## A03 — Software Supply Chain Failures

- Python vuln : `python/vulnerable/requirements.txt:1,2,5` — `Flask==2.0.1`,
  `Werkzeug==2.0.1`, `PyYAML==5.3.1` (versions anciennes à CVE connues).
- Java vuln : `java-spring/vulnerable/pom.xml:11,51-57` — `spring-boot 2.7.5`,
  `snakeyaml 1.30`, `commons-text 1.9` (ex. CVE-2022-1471, CVE-2022-42889).
- Démo : `pip-audit` / `mvn dependency:tree` + base CVE.
- Correctif Python : `requirements.txt` à jour et épinglé (Flask 3.1.3, Werkzeug
  3.1.8, Jinja2 3.1.6, requests 2.34.2, bcrypt 5.0.0, cryptography 50.0.1,
  itsdangerous 2.2.0 ; PyYAML retiré car inutile). `pip-audit` : 0 vulnérabilité
  (contre 57 sur la version vulnérable). Voir `DEMO-OUTILS-v1.2.md`.
- Correctif Java : `pom.xml` `spring-boot 3.3.4`, dépendances vulnérables retirées.

---

## A04 — Cryptographic Failures

- Python vuln :
  - `python/vulnerable/app/__init__.py:6` — clé secrète en dur.
  - `.../app/__init__.py:32-33` et `app/auth.py:15-16` — mots de passe en **MD5**.
  - IBAN stocké en clair (colonne `iban`).
- Java vuln :
  - `web/AuthController.java:25-26` et `config/DataLoader.java:23-24` — **MD5**.
  - IBAN en clair (`DataLoader.java:29`).
- Démo : lire la base ; un hash MD5 se casse instantanément (rainbow tables).
- Correctif Python : `security.py` bcrypt (coût 12) + Fernet pour l'IBAN ;
  secrets via `config.py` (environnement).
- Correctif Java : `SecurityConfig` BCrypt(12) + `security/CryptoService.java`
  (AES-GCM, clé dérivée d'un secret d'environnement) ; IBAN masqué à l'affichage.

---

## A05 — Injection

**Injection SQL**
- Python vuln : `python/vulnerable/app/tasks.py:41-42` — requête construite par
  concaténation (`… LIKE '%" + q + "%'`).
- Java vuln : `service/TaskSearchService.java:18-19` — `createNativeQuery` avec SQL
  concaténé.
- Démo : rechercher `%' OR '1'='1` (renvoie toutes les tâches).
- Correctif Python : `app/tasks.py` requête paramétrée + filtre propriétaire.
- Correctif Java : `TaskRepository.findByOwnerAndTitleContainingIgnoreCase` (requête
  dérivée paramétrée).

**XSS stocké**
- Python vuln : `templates/task_detail.html:15` — `{{ c["body"]|safe }}`.
- Java vuln : `templates/task_detail.html:18` et `preview.html:7` — `th:utext`.
- Démo : poster un commentaire `<script>alert(1)</script>`.
- Correctif Python : suppression de `|safe` (auto-échappement Jinja).
- Correctif Java : `th:text` à la place de `th:utext`.

---

## A06 — Insecure Design

- Python vuln :
  - `app/auth.py:47,51-52` — **mass assignment** : le `role` vient du formulaire.
  - `app/auth.py:88-96` — réinitialisation **énumérable** (message différent + jeton
    prévisible affiché).
  - Aucune politique de mot de passe.
- Java vuln :
  - `web/AuthController.java:59-65` — `role` accepté depuis la requête.
  - `reset()` révèle l'existence du compte.
- Démo : POST `/register` avec `role=ADMIN` ; comparer les réponses de `/reset`.
- Correctif Python : rôle forcé à `USER`, `password_is_strong()`, message de reset
  identique quel que soit le compte.
- Correctif Java : `AuthController` fixe `USER`, `STRONG_PWD`, message de reset unique.

---

## A07 — Authentication Failures

- Python vuln :
  - `app/auth.py:71` — jeton de session = `md5(username)` (prévisible).
  - `app/auth.py:73-75` — cookie « remember » = base64 de `user:md5(pass)`.
  - Aucune limitation de tentatives.
- Java vuln :
  - `web/AuthController.java:41` — cookie `token = md5(username)`.
  - `:42-45` — cookie « remember » réversible (base64).
  - Aucune limitation de tentatives.
- Démo : forger un cookie ; brute-force sans blocage.
- Correctif Python : sessions signées Flask, `session.clear()` (anti-fixation),
  rate limit (`_ATTEMPTS`), logout POST.
- Correctif Java : Spring Security (session recréée, anti-fixation),
  `LoginAttemptService` + `AppUserDetailsService` (verrouillage), logout POST.

---

## A08 — Software or Data Integrity Failures

- Python vuln : `app/tasks.py:2,16` — cookie `prefs` désérialisé par **pickle**
  (`pickle.loads(base64.b64decode(...))`) → RCE potentielle. Pas de CSRF.
- Java vuln : `web/TaskController.java:43-44` — cookie `prefs` désérialisé par
  **ObjectInputStream** (gadget chains). Pas de CSRF (form login manuel).
- Démo : forger un cookie `prefs` malveillant ; POST sans jeton anti-CSRF.
- Correctif Python : `itsdangerous.URLSafeSerializer` (JSON signé) + jeton CSRF
  vérifié dans `before_request`.
- Correctif Java : `security/PrefsCodec.java` (JSON Jackson signé HMAC) + CSRF
  Spring Security (actif par défaut).

---

## A09 — Logging & Alerting Failures

- Python vuln : `app/auth.py:65` — `log.info("… pass=%s", …, password)` : mot de
  passe en clair dans les journaux.
- Java vuln : `web/AuthController.java:36` — `System.out.println("… pass=" + password)`.
- Démo : lire la sortie console lors d'une connexion.
- Correctif Python : journal d'audit sans secret (`login_ok`, `login_echec`).
- Correctif Java : logger `taskforge.audit` dans les handlers de succès/échec, sans
  mot de passe.

---

## A10 — Mishandling of Exceptional Conditions

- Python vuln : `app/auth.py:27-35` — `login_required` : le `try/except` **échoue
  ouvert** (toute exception laisse passer la requête).
- Java vuln : `config/AuthInterceptor.java:20-22` — `catch (Exception) { return true; }`
  (fail-open) ; renvoie aussi `true` par défaut.
- Démo : provoquer une exception dans le chemin d'authentification.
- Correctif Python : `login_required` **échoue fermé** (redirection systématique si
  pas d'utilisateur), handlers d'erreurs génériques.
- Correctif Java : autorisation centralisée Spring Security (refus par défaut),
  `GlobalExceptionHandler` (page générique), `CryptoService.decrypt` renvoie vide en
  cas d'échec.

---

## Récapitulatif

| Réf | Python vuln | Java vuln | Fix Python | Fix Java |
|-----|-------------|-----------|------------|----------|
| A01 | tasks.py:49 / admin.py:9 / tasks.py:71 | TaskController:72 / AdminController:19 / TaskController:90 | `_owned_task_or_403`, `admin_required`, `url_is_safe` | `ownedTaskOr403`, SecurityConfig, `UrlSafety` |
| A02 | run.py:7 / __init__.py:23 | application.properties / WebConfig:20 | `secure_headers` | properties + `.headers(...)` |
| A03 | requirements.txt | pom.xml:11,51,56 | versions à jour | Boot 3.3.4 |
| A04 | __init__.py:6,32 / auth.py:15 | AuthController:25 / DataLoader:23 | bcrypt + Fernet | BCrypt + `CryptoService` |
| A05 | tasks.py:41 / task_detail.html:15 | TaskSearchService:18 / task_detail.html:18 | requête paramétrée / no `|safe` | requête dérivée / `th:text` |
| A06 | auth.py:47,88 | AuthController:59 | rôle forcé, reset unique | rôle forcé, `STRONG_PWD` |
| A07 | auth.py:71,74 | AuthController:41 | session signée + rate limit | Spring Security + `LoginAttemptService` |
| A08 | tasks.py:16 | TaskController:43 | JSON signé + CSRF | `PrefsCodec` + CSRF |
| A09 | auth.py:65 | AuthController:36 | audit sans secret | audit sans secret |
| A10 | auth.py:35 | AuthInterceptor:22 | fail-closed | authz centralisée + handler |
