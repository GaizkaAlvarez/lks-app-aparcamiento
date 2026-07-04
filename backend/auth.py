"""
Firebase ID token verification middleware for FastAPI.
Verifies the Bearer token on every protected request and extracts the user UID.
"""
from fastapi import Header, HTTPException, status

import firebase_admin
from firebase_admin import auth as firebase_auth

# Ensure Firebase Admin is initialized before first use
_initialized = False


def _ensure_init():
    global _initialized
    if not _initialized:
        try:
            firebase_admin.get_app()
        except ValueError:
            # Not yet initialized — firestore_client will handle it
            from firestore_client import _init_app
            _init_app()
        _initialized = True


async def verify_firebase_token(authorization: str = Header(...)) -> str:
    """
    FastAPI dependency that extracts and verifies the Firebase ID token
    from the Authorization header. Returns the authenticated user's UID.

    Raises 401 if the token is missing, invalid, or expired.
    """
    _ensure_init()

    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or malformed Authorization header. Expected: Bearer <token>",
        )

    token = authorization[len("Bearer "):].strip()

    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Empty token",
        )

    try:
        decoded = firebase_auth.verify_id_token(token)
        return decoded["uid"]
    except firebase_auth.ExpiredIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token expired. Please refresh your credentials.",
        )
    except firebase_auth.InvalidIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token.",
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Authentication failed: {str(e)}",
        )
