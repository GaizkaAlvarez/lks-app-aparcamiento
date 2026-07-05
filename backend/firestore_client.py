"""
Firestore client singleton initialized via Firebase Admin SDK (service account).
Uses GOOGLE_APPLICATION_CREDENTIALS env var to locate the JSON key file.
"""
import os

import firebase_admin
from firebase_admin import credentials, firestore

_app = None
_db = None


def _init_app():
    """Initialize Firebase Admin SDK if not already initialized."""
    global _app, _db
    if _app is not None:
        return

    cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "./service-account.json")

    if os.path.exists(cred_path):
        cred = credentials.Certificate(cred_path)
        _app = firebase_admin.initialize_app(cred)
    else:
        # Fallback to Application Default Credentials
        # (works on Cloud Run / GCP environments)
        _app = firebase_admin.initialize_app()

    _db = firestore.client()


def get_db():
    """Return the Firestore client singleton."""
    if _db is None:
        _init_app()
    return _db


def get_spots_collection():
    """Return reference to parkingSpots collection."""
    return get_db().collection("parkingSpots")


def get_reservations_collection():
    """Return reference to reservations collection."""
    return get_db().collection("reservations")


def get_users_collection():
    """Return reference to users collection."""
    return get_db().collection("users")
