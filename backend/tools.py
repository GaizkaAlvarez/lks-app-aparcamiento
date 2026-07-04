"""
LangChain tools that the agent can invoke to query Firestore.
Each function is decorated with @tool for automatic schema generation.
"""
from langchain_core.tools import tool

from firestore_client import get_spots_collection, get_reservations_collection, get_users_collection


def _time_to_minutes(t: str) -> int:
    """Convert HH:mm to total minutes since midnight."""
    parts = t.split(":")
    return int(parts[0]) * 60 + int(parts[1])


def _overlaps(start1: str, end1: str, start2: str, end2: str) -> bool:
    """Check if two time ranges [start1, end1) and [start2, end2) overlap."""
    s1 = _time_to_minutes(start1)
    e1 = _time_to_minutes(end1)
    s2 = _time_to_minutes(start2)
    e2 = _time_to_minutes(end2)
    return s1 < e2 and s2 < e1


@tool
def get_available_spots(vehicle_type: str, date: str, start_time: str, end_time: str) -> list[dict]:
    """
    Finds available parking spots compatible with the given vehicle type for a specific date and time range.

    Compatibility rules:
    - "combustion" vehicles → only combustion spots (numbers 1-21).
    - "electric" vehicles → electric spots (22-24) AND combustion spots (1-21).
    - "motorcycle" vehicles → only motorcycle spots (number 25).

    Args:
        vehicle_type: One of "combustion", "electric", "motorcycle".
        date: Date in yyyy-MM-dd format.
        start_time: Start time in HH:mm format.
        end_time: End time in HH:mm format.

    Returns:
        A list of available spots, each as a dict with keys: id, number, type.
    """
    vehicle = vehicle_type.lower().strip()

    # 1. Get all parking spots from Firestore
    spots_docs = get_spots_collection().stream()
    all_spots = []
    for doc in spots_docs:
        data = doc.to_dict()
        all_spots.append({
            "id": doc.id,
            "number": data.get("number", 0),
            "type": data.get("type", "combustion"),
            "available": data.get("available", True),
        })

    # 2. Filter by vehicle type compatibility
    if vehicle in ("motorcycle", "moto"):
        compatible_types = {"motorcycle"}
    elif vehicle in ("electric", "eléctrico", "electrico"):
        compatible_types = {"electric", "combustion"}
    else:  # combustion / default
        compatible_types = {"combustion"}

    compatible_spots = [
        s for s in all_spots
        if s["type"] in compatible_types and s["available"]
    ]

    # 3. Get conflicting reservations for the given date
    reservations_docs = get_reservations_collection() \
        .where("date", "==", date) \
        .where("status", "==", "active") \
        .stream()

    conflicting_spot_ids: set[str] = set()
    for doc in reservations_docs:
        data = doc.to_dict()
        r_start = data.get("startTime", "")
        r_end = data.get("endTime", "")
        if _overlaps(start_time, end_time, r_start, r_end):
            conflicting_spot_ids.add(data.get("spotId", ""))

    # 4. Return spots that are compatible AND not conflicting
    available = [
        {"id": s["id"], "number": s["number"], "type": s["type"]}
        for s in compatible_spots
        if s["id"] not in conflicting_spot_ids
    ]

    # Sort by number for consistent output
    available.sort(key=lambda s: s["number"])
    return available


@tool
def get_user_vehicles(user_id: str) -> list[dict]:
    """
    Gets the list of vehicles registered for a specific user.

    Args:
        user_id: The Firebase Auth UID of the user.

    Returns:
        A list of vehicle dicts, each with keys: id, licensePlate, type.
        Returns an empty list if the user has no vehicles or the profile doesn't exist.
    """
    doc = get_users_collection().document(user_id).get()
    if not doc.exists:
        return []

    data = doc.to_dict()
    vehicles = data.get("vehicles", [])
    if not vehicles:
        return []

    return [
        {
            "id": v.get("id", ""),
            "licensePlate": v.get("licensePlate", ""),
            "type": v.get("type", "normal"),
        }
        for v in vehicles
    ]
