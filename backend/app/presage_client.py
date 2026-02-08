"""
Lightweight Presage API client scaffolding.

This module reads `PRESAGE_API_KEY` and `PRESAGE_API_URL` from config and
exposes a helper to call Presage endpoints. Replace `endpoint` and payload
based on Presage SDK/API documentation.

Privacy note: Keep Presage keys server-side only.
"""
from typing import Any, Dict, Optional
import requests
from .config import PRESAGE_API_URL, get_presage_headers

class PresageClient:
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = base_url or PRESAGE_API_URL
        self.headers = get_presage_headers()

    def is_configured(self) -> bool:
        return bool(self.headers.get("Authorization"))

    def post(self, endpoint: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        if not self.is_configured():
            raise RuntimeError("Presage not configured: missing API key")
        url = f"{self.base_url.rstrip('/')}/{endpoint.lstrip('/')}"
        resp = requests.post(url, json=payload, headers=self.headers, timeout=10)
        resp.raise_for_status()
        return resp.json()

# Example usage placeholder:
# client = PresageClient()
# if client.is_configured():
#     data = client.post("v1/measures", {"source": "webcam", "signals": ["movement"]})
#     print(data)
