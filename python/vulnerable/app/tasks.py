import base64
import pickle
import requests
from flask import (Blueprint, request, redirect, url_for, session,
                   render_template)
from .db import get_db
from .auth import login_required, get_current_user

bp = Blueprint("tasks", __name__)


def load_prefs():
    raw = request.cookies.get("prefs")
    if not raw:
        return {"theme": "clair"}
    return pickle.loads(base64.b64decode(raw))


@bp.route("/")
def index():
    if session.get("user"):
        return redirect(url_for("tasks.list_tasks"))
    return redirect(url_for("auth.login"))


@bp.route("/tasks")
@login_required
def list_tasks():
    db = get_db()
    user = get_current_user()
    rows = db.execute("SELECT * FROM tasks WHERE owner = ?", (user["username"],)).fetchall()
    prefs = load_prefs()
    return render_template("tasks.html", tasks=rows, prefs=prefs, user=user)


@bp.route("/tasks/search")
@login_required
def search():
    q = request.args.get("q", "")
    db = get_db()
    query = "SELECT * FROM tasks WHERE title LIKE '%" + q + "%'"
    rows = db.execute(query).fetchall()
    return render_template("tasks.html", tasks=rows, prefs=load_prefs(),
                           user=get_current_user(), q=q)


@bp.route("/tasks/<int:task_id>")
@login_required
def detail(task_id):
    db = get_db()
    task = db.execute("SELECT * FROM tasks WHERE id = ?", (task_id,)).fetchone()
    comments = db.execute("SELECT * FROM comments WHERE task_id = ?", (task_id,)).fetchall()
    return render_template("task_detail.html", task=task, comments=comments,
                           user=get_current_user())


@bp.route("/tasks/<int:task_id>/comment", methods=["POST"])
@login_required
def comment(task_id):
    body = request.form["body"]
    user = get_current_user()
    db = get_db()
    db.execute("INSERT INTO comments (task_id, author, body) VALUES (?, ?, ?)",
               (task_id, user["username"], body))
    db.commit()
    return redirect(url_for("tasks.detail", task_id=task_id))


@bp.route("/tasks/<int:task_id>/preview", methods=["POST"])
@login_required
def preview(task_id):
    target = request.form["url"]
    content = ""
    try:
        r = requests.get(target, timeout=5)
        content = r.text[:2000]
    except Exception:
        pass
    return render_template("preview.html", content=content, target=target)
