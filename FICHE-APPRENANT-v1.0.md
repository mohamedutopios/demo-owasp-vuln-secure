# TaskForge — Fiche apprenant (v1.0)

## Table of Contents

<details>

   <summary>Contents</summary>

1. [Contexte](#contexte)
1. [Objectif](#objectif)
1. [Rappel des catégories OWASP Top 10:2025](#rappel-des-catgories-owasp-top-102025)
1. [Grille à compléter](#grille--complter)
   1. [A01 — Broken Access Control](#a01--broken-access-control)
   1. [A02 — Security Misconfiguration](#a02--security-misconfiguration)
   1. [A03 — Software Supply Chain Failures](#a03--software-supply-chain-failures)
   1. [A04 — Cryptographic Failures](#a04--cryptographic-failures)
   1. [A05 — Injection](#a05--injection)
   1. [A06 — Insecure Design](#a06--insecure-design)
   1. [A07 — Authentication Failures](#a07--authentication-failures)
   1. [A08 — Software or Data Integrity Failures](#a08--software-or-data-integrity-failures)
   1. [A09 — Logging & Alerting Failures](#a09--logging--alerting-failures)
   1. [A10 — Mishandling of Exceptional Conditions](#a10--mishandling-of-exceptional-conditions)
1. [Pistes de manipulation (sans dévoiler les failles)](#pistes-de-manipulation-sans-dvoiler-les-failles)

</details>

## Contexte

TaskForge est une petite application de gestion de tâches (comptes `admin`,
`alice`, `bob`). Elle est fournie en deux langages — **Python (Flask)** et
**Java (Spring Boot)** :

- `vulnerable/` : à auditer ;

L'architecture est volontairement soignée (séparation web / service / accès
données, gabarits, sessions) : rien ne saute aux yeux. Chaque version vulnérable
contient pourtant **exactement une faille par catégorie de l'OWASP Top 10:2025**.

## Objectif

1. Démarrer la version vulnérable (voir le `README.md` du dossier).
2. Identifier les 10 failles par **lecture du code** et **manipulation** de
   l'application.
3. Les cartographier sur la grille ci-dessous.
4. Proposer un correctif pour chacune, puis comparer avec la version `secure/`.

## Rappel des catégories OWASP Top 10:2025

| Réf | Catégorie |
|-----|-----------|
| A01 | Broken Access Control (inclut SSRF, IDOR/BOLA, BFLA) |
| A02 | Security Misconfiguration |
| A03 | Software Supply Chain Failures |
| A04 | Cryptographic Failures |
| A05 | Injection |
| A06 | Insecure Design |
| A07 | Authentication Failures |
| A08 | Software or Data Integrity Failures |
| A09 | Logging & Alerting Failures |
| A10 | Mishandling of Exceptional Conditions |

## Grille à compléter

Pour chaque catégorie : le fichier et la ligne concernés, la description de la
faille, une démonstration (URL, requête, manipulation) et le correctif proposé.

### A01 — Broken Access Control
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A02 — Security Misconfiguration
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A03 — Software Supply Chain Failures
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A04 — Cryptographic Failures
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A05 — Injection
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A06 — Insecure Design
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A07 — Authentication Failures
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A08 — Software or Data Integrity Failures
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A09 — Logging & Alerting Failures
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

### A10 — Mishandling of Exceptional Conditions
- Fichier / ligne :
- Description :
- Démonstration :
- Correctif proposé :

## Pistes de manipulation (sans dévoiler les failles)

- Connectez-vous avec un compte `USER` et observez les URL accessibles.
- Essayez d'accéder à des ressources qui ne vous appartiennent pas.
- Saisissez des caractères spéciaux dans les champs de recherche et de commentaire.
- Inspectez les cookies posés par l'application.
- Regardez les en-têtes de réponse HTTP.
- Consultez les fichiers de dépendances et les journaux au démarrage.
- Provoquez une erreur et observez ce qui est renvoyé.
