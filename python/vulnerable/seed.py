"""Initialise la base et injecte des donnees de demonstration."""
from app import create_app
from app.db import init_db, get_db
import hashlib


def md5(s):
    return hashlib.md5(s.encode()).hexdigest()


def main():
    app = create_app()
    with app.app_context():
        init_db()
        db = get_db()
        db.execute("DELETE FROM comments")
        db.execute("DELETE FROM tasks")
        db.execute("DELETE FROM users")
        db.executemany(
            "INSERT INTO users (username, password, role, iban) VALUES (?, ?, ?, ?)",
            [
                ("admin", md5("admin"), "ADMIN", "FR7630006000011234567890189"),
                ("alice", md5("password1"), "USER", "FR1420041010050500013M02606"),
                ("bob", md5("hunter2"), "USER", "FR7630004000031234567890143"),
            ],
        )
        db.executemany(
            "INSERT INTO tasks (title, description, owner) VALUES (?, ?, ?)",
            [
                ("Preparer la release", "Publier la version 1.2 du portail", "alice"),
                ("Revue budget", "Valider les depenses Q3", "alice"),
                ("Note RH confidentielle", "Augmentation de bob a valider", "admin"),
                ("Migration serveur", "Basculer sur le nouveau datacenter", "bob"),
            ],
        )
        db.commit()
        print("Base initialisee : admin/admin, alice/password1, bob/hunter2")


if __name__ == "__main__":
    main()
