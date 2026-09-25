import logging
from flask import Flask, render_template, request, abort
from . import config
from .db import close_db
from .security import csrf_token, csrf_valid

# A09 : journalisation des evenements de securite, sans donnee sensible
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
audit = logging.getLogger("taskforge.audit")


def create_app():
    app = Flask(__name__)
    app.config.update(
        SECRET_KEY=config.SECRET_KEY,
        SESSION_COOKIE_HTTPONLY=True,       # A07 : cookie inaccessible au JS
        SESSION_COOKIE_SAMESITE="Lax",      # A08 : attenue le CSRF
        SESSION_COOKIE_SECURE=False,        # A07 : passer a True derriere HTTPS
        PERMANENT_SESSION_LIFETIME=1800,
        MAX_CONTENT_LENGTH=1 * 1024 * 1024,
    )
    app.teardown_appcontext(close_db)

    from .auth import bp as auth_bp
    from .tasks import bp as tasks_bp
    from .admin import bp as admin_bp
    app.register_blueprint(auth_bp)
    app.register_blueprint(tasks_bp)
    app.register_blueprint(admin_bp)

    # A08 : verification systematique du jeton anti-CSRF sur les requetes d'etat
    @app.before_request
    def enforce_csrf():
        if request.method in ("POST", "PUT", "PATCH", "DELETE"):
            if not csrf_valid(request.form.get("csrf_token")):
                audit.warning("csrf_refuse path=%s ip=%s", request.path, request.remote_addr)
                abort(400)

    # A02 : en-tetes de securite, pas de CORS permissif, pas de banniere serveur
    @app.after_request
    def secure_headers(resp):
        resp.headers["X-Content-Type-Options"] = "nosniff"
        resp.headers["X-Frame-Options"] = "DENY"
        resp.headers["Referrer-Policy"] = "no-referrer"
        resp.headers["Content-Security-Policy"] = "default-src 'self'; style-src 'self' 'unsafe-inline'"
        resp.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        resp.headers.pop("Server", None)
        return resp

    # A02/A10 : messages d'erreur generiques, aucune trace divulguee
    @app.errorhandler(400)
    def bad_request(e):
        return render_template("error.html", code=400, message="Requete invalide."), 400

    @app.errorhandler(403)
    def forbidden(e):
        return render_template("error.html", code=403, message="Acces refuse."), 403

    @app.errorhandler(404)
    def not_found(e):
        return render_template("error.html", code=404, message="Ressource introuvable."), 404

    @app.errorhandler(Exception)
    def internal(e):
        audit.exception("erreur_non_geree path=%s", request.path)
        return render_template("error.html", code=500, message="Une erreur est survenue."), 500

    # Rendre le jeton CSRF disponible aux gabarits
    @app.context_processor
    def inject_csrf():
        return {"csrf_token": csrf_token}

    return app
