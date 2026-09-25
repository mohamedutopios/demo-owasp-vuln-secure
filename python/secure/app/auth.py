import time
from functools import wraps
from flask import (Blueprint, request, redirect, url_for, session,
                   render_template, g, abort)
from .db import get_db
from .security import (verify_password, hash_password, password_is_strong)
from . import audit

bp = Blueprint("auth", __name__)

# A06 : anti-force brute simple en memoire (en prod : Flask-Limiter + backend Redis)
_ATTEMPTS = {}
_MAX_ATTEMPTS = 5
_WINDOW = 300


def get_current_user():
    username = session.get("user")
    if not username:
        return None
    db = get_db()
    return db.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()


def login_required(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        # A10 : on echoue de maniere sure : toute absence d'utilisateur => redirection
        user = get_current_user()
        if user is None:
            return redirect(url_for("auth.login"))
        g.user = user
        return f(*args, **kwargs)
    return wrapper


def admin_required(f):
    @wraps(f)
    @login_required
    def wrapper(*args, **kwargs):
        # A01 : controle d'acces au niveau fonction
        if g.user["role"] != "ADMIN":
            audit.warning("acces_admin_refuse user=%s", g.user["username"])
            abort(403)
        return f(*args, **kwargs)
    return wrapper


def _rate_limited(key):
    now = time.time()
    hits = [t for t in _ATTEMPTS.get(key, []) if now - t < _WINDOW]
    _ATTEMPTS[key] = hits
    return len(hits) >= _MAX_ATTEMPTS


def _record_attempt(key):
    _ATTEMPTS.setdefault(key, []).append(time.time())


@bp.route("/register", methods=["GET", "POST"])
def register():
    error = None
    if request.method == "POST":
        username = request.form["username"].strip()
        password = request.form["password"]
        iban = request.form.get("iban", "").strip()
        if not username or not password_is_strong(password):
            error = ("Mot de passe trop faible : 12 caracteres minimum, "
                     "avec majuscule, minuscule et chiffre.")
        else:
            db = get_db()
            if db.execute("SELECT 1 FROM users WHERE username = ?", (username,)).fetchone():
                error = "Identifiant indisponible."
            else:
                from .security import encrypt_field
                # A06 : le role n'est jamais pris depuis le formulaire (pas de mass assignment)
                db.execute(
                    "INSERT INTO users (username, password, role, iban) VALUES (?, ?, 'USER', ?)",
                    (username, hash_password(password), encrypt_field(iban) if iban else None),
                )
                db.commit()
                audit.info("compte_cree user=%s", username)
                return redirect(url_for("auth.login"))
    return render_template("register.html", error=error)


@bp.route("/login", methods=["GET", "POST"])
def login():
    error = None
    if request.method == "POST":
        username = request.form["username"].strip()
        password = request.form["password"]
        key = f"{request.remote_addr}:{username}"
        if _rate_limited(key):
            audit.warning("login_bloque_ratelimit user=%s ip=%s", username, request.remote_addr)
            error = "Trop de tentatives. Reessayez dans quelques minutes."
            return render_template("login.html", error=error), 429
        db = get_db()
        user = db.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()
        # A04/A07 : comparaison via bcrypt ; message identique quel que soit l'echec
        if user and verify_password(password, user["password"]):
            session.clear()                 # A07 : nouvelle session (anti fixation)
            session["user"] = username
            session["role"] = user["role"]
            session.permanent = True
            audit.info("login_ok user=%s", username)   # A09 : pas de mot de passe journalise
            return redirect(url_for("tasks.list_tasks"))
        _record_attempt(key)
        audit.warning("login_echec user=%s ip=%s", username, request.remote_addr)
        error = "Identifiants invalides."
    return render_template("login.html", error=error)


@bp.route("/logout", methods=["POST"])
def logout():
    user = session.get("user")
    session.clear()                          # A07 : invalidation complete de la session
    audit.info("logout user=%s", user)
    return redirect(url_for("auth.login"))


@bp.route("/reset", methods=["GET", "POST"])
def reset():
    message = None
    if request.method == "POST":
        # A06 : reponse identique que le compte existe ou non (anti-enumeration).
        # Le jeton reel serait aleatoire, a duree limitee et envoye hors bande.
        audit.info("reset_demande")
        message = ("Si un compte correspond a cette adresse, un lien de "
                   "reinitialisation vient d'etre envoye.")
    return render_template("reset.html", message=message)
