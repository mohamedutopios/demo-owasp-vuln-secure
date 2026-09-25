from flask import Blueprint, render_template, redirect, url_for, g, abort
from .db import get_db
from .auth import admin_required
from .security import decrypt_field
from . import audit

bp = Blueprint("admin", __name__, url_prefix="/admin")


@bp.route("/users")
@admin_required
def users():
    db = get_db()
    rows = db.execute("SELECT id, username, role, iban FROM users").fetchall()
    # A04 : l'IBAN est dechiffre a l'affichage et masque (seuls les 4 derniers)
    users_view = []
    for r in rows:
        iban = decrypt_field(r["iban"]) if r["iban"] else ""
        masked = ("**** " + iban[-4:]) if iban else "-"
        users_view.append({"id": r["id"], "username": r["username"],
                           "role": r["role"], "iban": masked})
    return render_template("admin_users.html", users=users_view, user=g.user)


@bp.route("/users/<int:user_id>/delete", methods=["POST"])
@admin_required
def delete_user(user_id):
    if user_id == g.user["id"]:
        abort(400)
    db = get_db()
    db.execute("DELETE FROM users WHERE id = ?", (user_id,))
    db.commit()
    audit.info("user_supprime par=%s cible=%s", g.user["username"], user_id)
    return redirect(url_for("admin.users"))
