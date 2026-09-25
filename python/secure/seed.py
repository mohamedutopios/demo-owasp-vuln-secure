"""Initialise la base et injecte des donnees de demonstration (version conforme)."""
from app import create_app
from app.db import init_db, get_db
from app.security import hash_password, encrypt_field


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
                ("admin", hash_password("Admin!Passw0rd"), "ADMIN",
                 encrypt_field("FR7630006000011234567890189")),
                ("alice", hash_password("Alice!Passw0rd"), "USER",
                 encrypt_field("FR1420041010050500013M02606")),
                ("bob", hash_password("Bob!Passw0rd42"), "USER",
                 encrypt_field("FR7630004000031234567890143")),
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
        print("Base initialisee (conforme).")
        print("admin/Admin!Passw0rd  alice/Alice!Passw0rd  bob/Bob!Passw0rd42")


if __name__ == "__main__":
    main()
