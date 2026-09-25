# TaskForge - Python (Flask) - version VULNERABLE
# Image volontairement faillible, reservee au lab. Ne jamais exposer sur un reseau.
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
# Initialise la base SQLite et les comptes de demonstration au demarrage,
# puis lance le serveur de developpement (debug active : c'est le defaut A02).
EXPOSE 5000
CMD ["sh", "-c", "python seed.py && python run.py"]
