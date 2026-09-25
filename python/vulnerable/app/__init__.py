import hashlib
from flask import Flask
from .db import close_db

# Configuration applicative
SECRET_KEY = "taskforge-secret-2024"


def create_app():
    app = Flask(__name__)
    app.config["SECRET_KEY"] = SECRET_KEY
    app.teardown_appcontext(close_db)

    from .auth import bp as auth_bp
    from .tasks import bp as tasks_bp
    from .admin import bp as admin_bp

    app.register_blueprint(auth_bp)
    app.register_blueprint(tasks_bp)
    app.register_blueprint(admin_bp)

    # En-tetes communs a toutes les reponses
    @app.after_request
    def add_headers(resp):
        resp.headers["Access-Control-Allow-Origin"] = "*"
        resp.headers["Server"] = "TaskForge/1.0 Python/3.11 Flask/2.0.1"
        return resp

    return app


def md5(s):
    return hashlib.md5(s.encode()).hexdigest()
