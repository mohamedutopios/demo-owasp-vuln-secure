# TaskForge — version Python (Flask) — À AUDITER

> Application volontairement construite pour l'exercice d'audit. Ne pas déployer.

## Prérequis (étape 0)

- Python 3.11 ou supérieur installé (`python3 --version`)
- Accès Internet pour l'installation des dépendances

## Démarrage

```bash
# 1. Se placer dans le dossier de l'application
cd python/vulnerable

# 2. Créer et activer un environnement virtuel dédié
python3 -m venv .venv
source .venv/bin/activate          # Windows : .venv\Scripts\activate

# 3. Installer les dépendances
pip install -r requirements.txt

# 4. Initialiser la base et les comptes de démonstration
python seed.py

# 5. Lancer l'application
python run.py
```

Application disponible sur http://localhost:5050

## Comptes de démonstration

| Identifiant | Mot de passe | Rôle  |
|-------------|--------------|-------|
| admin       | admin        | ADMIN |
| alice       | password1    | USER  |
| bob         | hunter2      | USER  |

## Exercice

Le code de cette application ressemble à celui d'une application correctement
structurée (séparation `web` / `service` / accès données, gabarits, sessions).
Elle contient néanmoins **une faille par catégorie de l'OWASP Top 10:2025**.
Objectif : les identifier une par une par lecture du code et manipulation de
l'application, puis les cartographier sur la grille A01 → A10.

Reportez vos trouvailles sur la fiche apprenant (`FICHE-APPRENANT-v1.0.md`).
