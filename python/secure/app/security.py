"""Primitives de securite reutilisables (A04, A01/SSRF, A06, A07, A08)."""
import ipaddress
import re
import secrets
import socket
from urllib.parse import urlparse

import bcrypt
from cryptography.fernet import Fernet
from flask import session

from . import config

_fernet = Fernet(config.ENC_KEY.encode())

# --- A04 : hachage des mots de passe (bcrypt, cout 12) ---

def hash_password(plain: str) -> str:
    return bcrypt.hashpw(plain.encode(), bcrypt.gensalt(rounds=12)).decode()


def verify_password(plain: str, stored: str) -> bool:
    try:
        return bcrypt.checkpw(plain.encode(), stored.encode())
    except (ValueError, TypeError):
        # A10 : en cas de hash illisible on echoue de maniere sure (refus)
        return False


# --- A04 : chiffrement des donnees sensibles au repos (IBAN) ---

def encrypt_field(value: str) -> str:
    return _fernet.encrypt(value.encode()).decode()


def decrypt_field(token: str) -> str:
    try:
        return _fernet.decrypt(token.encode()).decode()
    except Exception:
        return ""


# --- A06/A07 : politique de mot de passe ---
_PWD_RE = re.compile(r"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{12,}$")


def password_is_strong(pwd: str) -> bool:
    return bool(_PWD_RE.match(pwd))


# --- A08 : jeton anti-CSRF lie a la session ---

def csrf_token() -> str:
    if "csrf" not in session:
        session["csrf"] = secrets.token_urlsafe(32)
    return session["csrf"]


def csrf_valid(submitted: str) -> bool:
    expected = session.get("csrf")
    return bool(expected) and secrets.compare_digest(expected, submitted or "")


# --- A01 (SSRF) : validation stricte de l'URL cible ---
_ALLOWED_SCHEMES = {"http", "https"}


def url_is_safe(raw: str) -> bool:
    try:
        parsed = urlparse(raw)
    except ValueError:
        return False
    if parsed.scheme not in _ALLOWED_SCHEMES or not parsed.hostname:
        return False
    try:
        infos = socket.getaddrinfo(parsed.hostname, None)
    except socket.gaierror:
        return False
    for family, _, _, _, sockaddr in infos:
        ip = ipaddress.ip_address(sockaddr[0])
        if (ip.is_private or ip.is_loopback or ip.is_link_local
                or ip.is_reserved or ip.is_multicast):
            return False
    return True
