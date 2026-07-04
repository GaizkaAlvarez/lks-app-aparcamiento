"""
LangChain agent powered by Gemini for the parking assistant chatbot.
Handles the tool-calling loop: model → tool calls → execute → feed back → final response.
"""
import os
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_google_genai import ChatGoogleGenerativeAI

from tools import get_available_spots, get_user_vehicles

# Load .env from backend/ or parent directory (project root)
_BACKEND_DIR = Path(__file__).resolve().parent
for _env_dir in (_BACKEND_DIR, _BACKEND_DIR.parent):
    _env_path = _env_dir / ".env"
    if _env_path.exists():
        load_dotenv(_env_path)
        break
else:
    load_dotenv()  # fallback to default behaviour

SYSTEM_PROMPT = """Eres un asistente de aparcamiento inteligente para la app "LKS Parking". Ayudas a usuarios a encontrar y reservar plazas de aparcamiento. Hablas siempre en español, con un tono amable y profesional.

## REGLAS IMPORTANTES:

1. **Pregunta antes de actuar** si faltan datos: tipo de vehículo, fecha, hora de inicio, hora de fin.
   - Si el usuario no especifica hora de fin, asume 1 hora de duración desde la hora de inicio.

2. **Tipos de vehículo válidos**: "combustion", "electric", "motorcycle".
   - Si el usuario dice "coche normal" o "coche de gasolina" → "combustion".
   - Si el usuario dice "coche eléctrico" o "eléctrico" → "electric".
   - Si el usuario dice "moto" o "motocicleta" → "motorcycle".

3. **Compatibilidad de plazas**:
   - Plazas de combustión (números 1-21): solo vehículos de combustión.
   - Plazas eléctricas (números 22-24): vehículos eléctricos Y de combustión.
   - Plaza de moto (número 25): solo motos.

4. **Antes de recomendar plazas**, usa la herramienta `get_user_vehicles` para conocer los vehículos del usuario. Si el usuario tiene vehículos registrados, sugiérele usar uno de ellos.

5. **Cuando encuentres plazas disponibles**, preséntalas de forma clara indicando número, tipo y por qué son buena opción.

6. **Si no hay plazas disponibles**, sugiere alternativas concretas (otro día, otra franja horaria, u otro tipo de vehículo si aplica).

7. **NUNCA inventes datos**. Solo recomiendas plazas que las herramientas te devuelvan.

8. **Sé conciso**. El usuario está en una app móvil, no en un ensayo.

9. **Para fechas**, asume siempre fechas futuras. Si el usuario dice "mañana", calcula la fecha correcta basándote en la fecha actual. El formato de fecha es yyyy-MM-dd.

10. **Horarios**: el parking abre de 06:00 a 22:55. Las horas van en formato HH:mm (24h).
"""

# Tools available to the agent
TOOLS = [get_available_spots, get_user_vehicles]

# Map tool names → functions for execution
TOOL_MAP = {
    "get_available_spots": get_available_spots,
    "get_user_vehicles": get_user_vehicles,
}

MAX_TOOL_ITERATIONS = 3  # Safety limit to prevent infinite loops


def _build_messages(
    conversation_history: list[dict[str, str]],
    new_message: str,
) -> list:
    """Build the full message list for the model call."""
    messages = [SystemMessage(content=SYSTEM_PROMPT)]

    for entry in conversation_history:
        if entry["role"] == "user":
            messages.append(HumanMessage(content=entry["content"]))
        elif entry["role"] == "assistant":
            messages.append(AIMessage(content=entry["content"]))

    messages.append(HumanMessage(content=new_message))
    return messages


async def process_chat_message(
    message: str,
    user_id: str,
    conversation_history: list[dict[str, str]] | None = None,
) -> dict[str, Any]:
    """
    Process a chat message through the LangChain agent.

    Args:
        message: The user's latest message text.
        user_id: The authenticated user's Firebase UID.
        conversation_history: Previous messages in the conversation.

    Returns:
        A dict with keys: reply (str), recommendations (list[dict]).
    """
    if conversation_history is None:
        conversation_history = []

    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return {
            "reply": "Error de configuración: no se ha encontrado la clave API de Gemini. Contacta con el administrador.",
            "recommendations": [],
        }

    model = ChatGoogleGenerativeAI(
        model=os.getenv("GEMINI_MODEL", "gemini-2.0-flash"),
        google_api_key=api_key,
        temperature=0.2,
        max_tokens=1024,
    )
    model_with_tools = model.bind_tools(TOOLS)

    messages = _build_messages(conversation_history, message)

    # Inject user_id into tool calls automatically where needed
    # The tools that need user_id will receive it from the tool_args

    all_tool_outputs: list[dict] = []
    last_reservation_context: dict[str, str] = {}

    for iteration in range(MAX_TOOL_ITERATIONS + 1):
        response = await model_with_tools.ainvoke(messages)
        messages.append(response)

        # Check for tool calls
        tool_calls = getattr(response, "tool_calls", None) or []

        if not tool_calls:
            # No tool calls — this is the final response
            reply = response.content if hasattr(response, "content") else str(response)

            # Collect recommendations from tool outputs
            recommendations = _extract_recommendations(all_tool_outputs)

            return {
                "reply": reply,
                "recommendations": recommendations,
                "reservation_context": last_reservation_context if last_reservation_context else None,
            }

        # Execute each tool call
        for tc in tool_calls:
            tool_name = tc.get("name", "")
            tool_args = tc.get("args", {})

            # Track reservation context from get_available_spots calls
            if tool_name == "get_available_spots":
                last_reservation_context = {
                    "vehicle_type": tool_args.get("vehicle_type", ""),
                    "date": tool_args.get("date", ""),
                    "start_time": tool_args.get("start_time", ""),
                    "end_time": tool_args.get("end_time", ""),
                }

            # Inject user_id for tools that need it
            if tool_name == "get_user_vehicles" and "user_id" not in tool_args:
                tool_args["user_id"] = user_id

            tool_func = TOOL_MAP.get(tool_name)
            if tool_func is None:
                tool_result = f"Error: unknown tool '{tool_name}'"
            else:
                try:
                    result = tool_func.invoke(tool_args)
                    # Store structured results for recommendation extraction
                    if tool_name == "get_available_spots":
                        if isinstance(result, list):
                            all_tool_outputs.extend(result)
                    elif tool_name == "get_user_vehicles":
                        pass  # User vehicles are informational, not recommendations

                    tool_result = str(result)
                except Exception as e:
                    tool_result = f"Error executing {tool_name}: {str(e)}"

            messages.append(ToolMessage(content=tool_result, tool_call_id=tc.get("id", "")))

    # Fallback: if we hit the iteration limit, return the last response
    last_content = ""
    for msg in reversed(messages):
        if hasattr(msg, "content") and msg.content:
            last_content = str(msg.content)
            break

    return {
        "reply": last_content or "Lo siento, no he podido procesar tu solicitud. ¿Puedes intentarlo de nuevo?",
        "recommendations": _extract_recommendations(all_tool_outputs),
        "reservation_context": last_reservation_context if last_reservation_context else None,
    }


def _extract_recommendations(tool_outputs: list[dict]) -> list[dict]:
    """Extract and deduplicate spot recommendations from tool outputs."""
    seen: set[str] = set()
    recommendations: list[dict] = []
    for spot in tool_outputs:
        spot_id = spot.get("id", "")
        if spot_id and spot_id not in seen:
            seen.add(spot_id)
            recommendations.append({
                "number": spot.get("number", 0),
                "type": spot.get("type", "combustion"),
                "id": spot_id,
            })
    return recommendations
