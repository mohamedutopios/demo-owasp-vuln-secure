# TaskForge — version Python (Flask) — CONFORME OWASP Top 10:2025

Version corrigée de l'application, à comparer ligne à ligne avec la version
vulnérable. Chaque correctif est annoté dans le code par la catégorie concernée
(`# A0x - ...`).

## Prérequis (étape 0)

- Python 3.11 ou supérieur (`python3 --version`)
- Accès Internet pour l'installation des dépendances

## Démarrage

```bash
cd python/secure

python3 -m venv .venv
source .venv/bin/activate          # Windows : .venv\Scripts\activate

pip install -r requirements.txt

# Secrets : fournis par l'environnement en production.
# En lab, s'ils sont absents, des clés de développement sont générées dans instance/.
export TASKFORGE_SECRET="$(python -c 'import os;print(os.urandom(32).hex())')"
export TASKFORGE_ENC_KEY="$(python -c 'from cryptography.fernet import Fernet;print(Fernet.generate_key().decode())')"

python seed.py
python run.py
```

Application sur http://localhost:5000

## Comptes de démonstration

| Identifiant | Mot de passe     | Rôle  |
|-------------|------------------|-------|
| admin       | Admin!Passw0rd   | ADMIN |
| alice       | Alice!Passw0rd   | USER  |
| bob         | Bob!Passw0rd42   | USER  |

## Vérifier la chaîne d'approvisionnement (A03)

```bash
pip install pip-audit
pip-audit -r requirements.txt
```
