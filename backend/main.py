"""
FastAPI application for the Parkly AI Chatbot backend.
Exposes /api/chat for conversational AI and /api/reservations for creating reservations.
"""
import logging
import os
from datetime import datetime, timezone
from pathlib import Path

from dotenv import load_dotenv
from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware

# Load .env from backend/ or parent directory (project root)
_BACKEND_DIR = Path(__file__).resolve().parent
for _env_dir in (_BACKEND_DIR, _BACKEND_DIR.parent):
    _env_path = _env_dir / ".env"
    if _env_path.exists():
        load_dotenv(_env_path)
        break
else:
    load_dotenv()  # fallback to default behaviour

from agent import process_chat_message
from auth import verify_firebase_token
from firestore_client import get_reservations_collection, get_spots_collection, get_users_collection
from models import (
    ChatRequest,
    ChatResponse,
    CreateReservationRequest,
    CreateReservationResponse,
    ReservationContext,
)

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Parkly AI Chatbot API",
    description="Backend for the LKS Parking AI assistant chatbot",
    version="1.0.0",
)

# CORS — allow all origins for development; restrict in production
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Health check ─────────────────────────────────────────────────────

@app.get("/api/health")
async def health():
    """Simple health check endpoint."""
    gemini_configured = bool(os.getenv("GEMINI_API_KEY"))
    return {
        "status": "ok",
        "gemini_configured": gemini_configured,
    }


# ── Chat ─────────────────────────────────────────────────────────────

@app.post("/api/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    uid: str = Depends(verify_firebase_token),
):
    """
    Process a chat message through the LangChain + Gemini agent.
    Returns the AI's text reply and any parking spot recommendations.
    """
    # Verify the authenticated user matches the request
    if uid != request.user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Authenticated user does not match request user_id.",
        )

    logger.info(f"Chat request from user {uid}: {request.message[:100]}...")

    try:
        result = await process_chat_message(
            message=request.message,
            user_id=request.user_id,
            conversation_history=[
                {"role": m.role, "content": m.content}
                for m in request.conversation_history
            ],
        )

        ctx = result.get("reservation_context")
        reservation_context = None
        if ctx:
            reservation_context = ReservationContext(
                vehicle_type=ctx.get("vehicle_type", ""),
                date=ctx.get("date", ""),
                start_time=ctx.get("start_time", ""),
                end_time=ctx.get("end_time", ""),
            )

        return ChatResponse(
            reply=result["reply"],
            recommendations=[
                {"number": r["number"], "type": r["type"], "id": r["id"]}
                for r in result.get("recommendations", [])
            ],
            reservation_context=reservation_context,
        )
    except Exception as e:
        logger.exception("Error processing chat message")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al procesar el mensaje: {str(e)}",
        )


# ── Reservation creation ─────────────────────────────────────────────

def _time_to_minutes(t: str) -> int:
    parts = t.split(":")
    return int(parts[0]) * 60 + int(parts[1])


@app.post("/api/reservations", response_model=CreateReservationResponse)
async def create_reservation(
    request: CreateReservationRequest,
    uid: str = Depends(verify_firebase_token),
):
    """
    Create a parking reservation directly in Firestore.
    Performs conflict checking before writing.
    """
    if uid != request.user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Authenticated user does not match request user_id.",
        )

    logger.info(f"Reservation request from user {uid}: spot={request.spot_id}, date={request.date}")

    try:
        # 1. Verify the spot exists and is of a compatible type for the user's vehicle
        spot_doc = get_spots_collection().document(request.spot_id).get()
        if not spot_doc.exists:
            return CreateReservationResponse(
                success=False,
                error="La plaza seleccionada no existe.",
            )

        spot_data = spot_doc.to_dict()
        spot_number = spot_data.get("number", 0)
        spot_type = spot_data.get("type", "combustion")

        # 2. Verify user has the specified vehicle
        user_doc = get_users_collection().document(request.user_id).get()
        vehicles = []
        if user_doc.exists:
            vehicles = user_doc.to_dict().get("vehicles", [])

        vehicle = next((v for v in vehicles if v.get("id") == request.vehicle_id), None)
        if not vehicle:
            # If no specific vehicle match, use first vehicle (more lenient)
            vehicle = vehicles[0] if vehicles else None

        if not vehicle:
            return CreateReservationResponse(
                success=False,
                error="No se ha encontrado el vehículo. Añade un vehículo en tu perfil.",
            )

        # 3. Check for conflicting reservations
        new_start = _time_to_minutes(request.start_time)
        new_end = _time_to_minutes(request.end_time)

        reservations = get_reservations_collection() \
            .where("spotId", "==", request.spot_id) \
            .where("date", "==", request.date) \
            .where("status", "==", "active") \
            .stream()

        for doc in reservations:
            data = doc.to_dict()
            r_start = _time_to_minutes(data.get("startTime", "00:00"))
            r_end = _time_to_minutes(data.get("endTime", "00:00"))
            if new_start < r_end and new_end > r_start:
                return CreateReservationResponse(
                    success=False,
                    error="La plaza ya está reservada en ese horario. Pide al asistente que te busque otra opción.",
                )

        # 4. Create the reservation
        reservation_data = {
            "userId": request.user_id,
            "vehicleId": request.vehicle_id,
            "spotId": request.spot_id,
            "spotNumber": spot_number,
            "spotType": spot_type,
            "date": request.date,
            "startTime": request.start_time,
            "endTime": request.end_time,
            "status": "active",
            "createdAt": datetime.now(timezone.utc).isoformat(),
        }

        doc_ref = get_reservations_collection().document()
        doc_ref.set(reservation_data)

        logger.info(f"Reservation created: {doc_ref.id}")

        return CreateReservationResponse(
            success=True,
            reservation_id=doc_ref.id,
        )

    except Exception as e:
        logger.exception("Error creating reservation")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al crear la reserva: {str(e)}",
        )
