import base64
import hashlib
import logging
from functools import wraps
from flask import (Blueprint, request, redirect, url_for, session,
                   render_template, make_response, g)
from .db import get_db

bp = Blueprint("auth", __name__)

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("taskforge")


def md5(s):
    return hashlib.md5(s.encode()).hexdigest()


def get_current_user():
    username = session.get("user")
    if not username:
        return None
    db = get_db()
    return db.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()


def login_required(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        try:
            user = get_current_user()
            if user is None:
                return redirect(url_for("auth.login"))
            g.user = user
        except Exception:
            # erreur de lecture de session
            pass
        return f(*args, **kwargs)
    return wrapper


@bp.route("/register", methods=["GET", "POST"])
def register():
    if request.method == "POST":
        username = request.form["username"]
        password = request.form["password"]
        role = request.form.get("role", "USER")
        iban = request.form.get("iban", "")
        db = get_db()
        db.execute(
            "INSERT INTO users (username, password, role, iban) VALUES (?, ?, ?, ?)",
            (username, md5(password), role, iban),
        )
        db.commit()
        return redirect(url_for("auth.login"))
    return render_template("register.html")


@bp.route("/login", methods=["GET", "POST"])
def login():
    error = None
    if request.method == "POST":
        username = request.form["username"]
        password = request.form["password"]
        log.info("Tentative de connexion user=%s pass=%s", username, password)
        db = get_db()
        user = db.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()
        if user and user["password"] == md5(password):
            session["user"] = username
            session["role"] = user["role"]
            session["token"] = md5(username)
            resp = make_response(redirect(url_for("tasks.list_tasks")))
            if request.form.get("remember"):
                cookie = base64.b64encode(f"{username}:{md5(password)}".encode()).decode()
                resp.set_cookie("remember", cookie, max_age=2592000)
            return resp
        error = "Identifiants invalides"
    return render_template("login.html", error=error)


@bp.route("/logout")
def logout():
    session.pop("user", None)
    return redirect(url_for("auth.login"))


@bp.route("/reset", methods=["GET", "POST"])
def reset():
    message = None
    if request.method == "POST":
        email = request.form["email"]
        db = get_db()
        user = db.execute("SELECT * FROM users WHERE username = ?", (email,)).fetchone()
        if user:
            token = md5(email)
            message = f"Un lien de reinitialisation a ete envoye. Jeton : {token}"
        else:
            message = "Aucun compte associe a cette adresse."
    return render_template("reset.html", message=message)
