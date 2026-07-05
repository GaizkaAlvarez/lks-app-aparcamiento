"""
Pydantic models for request/response schemas.
"""
from pydantic import BaseModel, Field


# ── Chat ─────────────────────────────────────────────────────────────

class MessageEntry(BaseModel):
    """A single message in the conversation history."""
    role: str  # "user" or "assistant"
    content: str


class ChatRequest(BaseModel):
    """Request body for POST /api/chat."""
    message: str = Field(..., min_length=1, max_length=2000)
    user_id: str = Field(..., min_length=1)
    conversation_history: list[MessageEntry] = Field(default_factory=list)


class SpotRecommendation(BaseModel):
    """A single parking spot recommendation."""
    number: int
    type: str
    id: str


class ReservationContext(BaseModel):
    """Parameters extracted from the last get_available_spots tool call.
    These are the date/time/vehicle_type that the recommendations are based on."""
    vehicle_type: str = ""
    date: str = ""
    start_time: str = ""
    end_time: str = ""


class ChatResponse(BaseModel):
    """Response body for POST /api/chat."""
    reply: str
    recommendations: list[SpotRecommendation] = Field(default_factory=list)
    reservation_context: ReservationContext | None = None


# ── Reservations ──────────────────────────────────────────────────────

class CreateReservationRequest(BaseModel):
    """Request body for POST /api/reservations."""
    spot_id: str = Field(..., min_length=1)
    vehicle_id: str = Field(..., min_length=1)
    date: str = Field(..., min_length=1, description="yyyy-MM-dd")
    start_time: str = Field(..., min_length=1, description="HH:mm")
    end_time: str = Field(..., min_length=1, description="HH:mm")
    user_id: str = Field(..., min_length=1)


class CreateReservationResponse(BaseModel):
    """Response body for POST /api/reservations."""
    success: bool
    reservation_id: str | None = None
    error: str | None = None
