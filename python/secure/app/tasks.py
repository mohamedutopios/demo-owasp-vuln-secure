import json
import requests
from itsdangerous import URLSafeSerializer, BadSignature
from flask import (Blueprint, request, redirect, url_for, session,
                   render_template, g, abort, make_response)
from .db import get_db
from .auth import login_required, get_current_user
from .security import url_is_safe
from . import config, audit

bp = Blueprint("tasks", __name__)

# A08 : preferences serialisees en JSON signe (jamais de pickle)
_prefs_signer = URLSafeSerializer(config.SECRET_KEY, salt="prefs")


def load_prefs():
    raw = request.cookies.get("prefs")
    if not raw:
        return {"theme": "clair"}
    try:
        data = _prefs_signer.loads(raw)
        return data if isinstance(data, dict) else {"theme": "clair"}
    except BadSignature:
        return {"theme": "clair"}


def _owned_task_or_403(task_id):
    db = get_db()
    task = db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
    if task is None:
        abort(404)
    # A01 : controle d'acces au niveau objet (anti-IDOR/BOLA)
    if task["owner"] != g.user["username"] and g.user["role"] != "ADMIN":
        audit.warning("acces_objet_refuse user=%s task=%s", g.user["username"], task_id)
        abort(403)
    return task


@bp.route("/")
def index():
    if session.get("user"):
        return redirect(url_for("tasks.list_tasks"))
    return redirect(url_for("auth.login"))


@bp.route("/tasks")
@login_required
def list_tasks():
    db = get_db()
    rows = db.execute("SELECT * FROM tasks WHERE owner = ?", (g.user["username"],)).fetchall()
    return render_template("tasks.html", tasks=rows, prefs=load_prefs(), user=g.user)


@bp.route("/tasks/search")
@login_required
def search():
    q = request.args.get("q", "")
    db = get_db()
    # A05 : requete parametree + filtrage sur le proprietaire
    rows = db.execute(
        "SELECT * FROM tasks WHERE owner = ? AND title LIKE ?",
        (g.user["username"], f"%{q}%"),
    ).fetchall()
    return render_template("tasks.html", tasks=rows, prefs=load_prefs(), user=g.user, q=q)


@bp.route("/tasks/<int:task_id>")
@login_required
def detail(task_id):
    task = _owned_task_or_403(task_id)
    db = get_db()
    comments = db.execute("SELECT * FROM comments WHERE task_id = ?", (task_id,)).fetchall()
    return render_template("task_detail.html", task=task, comments=comments, user=g.user)


@bp.route("/tasks/<int:task_id>/comment", methods=["POST"])
@login_required
def comment(task_id):
    _owned_task_or_403(task_id)
    body = request.form["body"]
    db = get_db()
    # A05 : requete parametree ; l'echappement XSS est assure par l'auto-escape des gabarits
    db.execute("INSERT INTO comments (task_id, author, body) VALUES (?, ?, ?)",
               (task_id, g.user["username"], body))
    db.commit()
    return redirect(url_for("tasks.detail", task_id=task_id))


@bp.route("/tasks/<int:task_id>/preview", methods=["POST"])
@login_required
def preview(task_id):
    _owned_task_or_403(task_id)
    target = request.form["url"]
    # A01 (SSRF) : schema http/https uniquement, resolution DNS et blocage des plages privees
    if not url_is_safe(target):
        audit.warning("ssrf_refuse user=%s url=%s", g.user["username"], target)
        abort(400)
    content = ""
    try:
        r = requests.get(target, timeout=5, allow_redirects=False,
                         headers={"User-Agent": "TaskForge-preview"})
        # A05 : on n'affiche que le titre extrait, pas le HTML brut
        content = r.headers.get("Content-Type", "inconnu")
    except requests.RequestException:
        # A10 : erreur specifique geree, message generique
        content = "indisponible"
    return render_template("preview.html", content=content, target=target)


@bp.route("/prefs", methods=["POST"])
@login_required
def save_prefs():
    theme = "sombre" if request.form.get("theme") == "sombre" else "clair"
    cookie = _prefs_signer.dumps({"theme": theme})
    resp = make_response(redirect(url_for("tasks.list_tasks")))
    resp.set_cookie("prefs", cookie, httponly=True, samesite="Lax")
    return resp
