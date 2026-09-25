# TaskForge - Python (Flask) - version CONFORME OWASP Top 10:2025
FROM python:3.12-slim

# Utilisateur non root (durcissement conteneur)
RUN useradd --create-home --uid 10001 appuser
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt gunicorn==23.0.0

COPY . .
RUN chown -R appuser:appuser /app
USER appuser

# Les secrets DOIVENT etre fournis par l'environnement (voir docker-compose / -e).
# Valeurs de repli generees au demarrage si absentes (pour le lab uniquement).
EXPOSE 5000
CMD ["sh", "-c", "python seed.py && gunicorn -b 0.0.0.0:5000 'app:create_app()'"]
