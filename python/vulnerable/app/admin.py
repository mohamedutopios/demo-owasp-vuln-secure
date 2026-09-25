from flask import Blueprint, render_template, request, redirect, url_for
from .db import get_db
from .auth import login_required, get_current_user

bp = Blueprint("admin", __name__, url_prefix="/admin")


@bp.route("/users")
@login_required
def users():
    db = get_db()
    rows = db.execute("SELECT id, username, role, iban FROM users").fetchall()
    return render_template("admin_users.html", users=rows, user=get_current_user())


@bp.route("/users/<int:user_id>/delete", methods=["POST"])
@login_required
def delete_user(user_id):
    db = get_db()
    db.execute("DELETE FROM users WHERE id = ?", (user_id,))
    db.commit()
    return redirect(url_for("admin.users"))
