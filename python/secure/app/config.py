"""Configuration : secrets et cles issus de l'environnement (jamais du code source).

Pour un usage hors production (lab), si les variables ne sont pas definies, des
cles aleatoires sont generees et conservees dans instance/ afin que
l'application demarre. En production, ces variables DOIVENT etre fournies par
l'environnement (coffre-fort de secrets) et le bloc de secours doit etre retire.
"""
import os
from pathlib import Path
from cryptography.fernet import Fernet

INSTANCE = Path(__file__).resolve().parent.parent / "instance"
INSTANCE.mkdir(exist_ok=True)


def _persisted(name, generator):
    env = os.environ.get(name)
    if env:
        return env
    f = INSTANCE / f"{name.lower()}.key"
    if f.exists():
        return f.read_text().strip()
    value = generator()
    f.write_text(value)
    return value


SECRET_KEY = _persisted("TASKFORGE_SECRET", lambda: os.urandom(32).hex())
ENC_KEY = _persisted("TASKFORGE_ENC_KEY", lambda: Fernet.generate_key().decode())
