"""
HistAR History AI - Python service (Ollama + RAG-ready).
BE calls POST /api/chat with {"prompt": "..."} -> {"reply": "..."}
"""
from __future__ import annotations

import os
from typing import Any

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:3b-instruct")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT_SECONDS", "120"))

app = FastAPI(title="HistAR History AI", version="0.1.0")


class ChatRequest(BaseModel):
    prompt: str = Field(min_length=1)


class ChatResponse(BaseModel):
    reply: str
    provider: str = "ollama"


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "history-ai"}


@app.post("/api/chat", response_model=ChatResponse)
def chat(req: ChatRequest) -> ChatResponse:
    body: dict[str, Any] = {
        "model": OLLAMA_MODEL,
        "prompt": req.prompt,
        "stream": False,
    }
    try:
        with httpx.Client(base_url=OLLAMA_BASE_URL, timeout=OLLAMA_TIMEOUT) as client:
            resp = client.post("/api/generate", json=body)
            resp.raise_for_status()
            data = resp.json()
    except httpx.HTTPError as exc:
        raise HTTPException(
            status_code=503,
            detail=f"Ollama unavailable at {OLLAMA_BASE_URL}: {exc}",
        ) from exc

    reply = (data.get("response") or "").strip()
    if not reply:
        raise HTTPException(status_code=502, detail="Ollama returned empty response")
    return ChatResponse(reply=reply)
