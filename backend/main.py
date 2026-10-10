import os
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from openai import OpenAI

load_dotenv()
app = FastAPI(title="FRIDAY AI Backend", version="1.0.0")
MODEL = os.getenv("OPENAI_MODEL", "gpt-4.1-mini")
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY")) if os.getenv("OPENAI_API_KEY") else None

class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=8000)
    language: str = Field(default="auto", max_length=40)

class ChatResponse(BaseModel):
    reply: str

@app.get("/health")
def health():
    return {"status": "ok", "ai_configured": client is not None}

@app.post("/chat", response_model=ChatResponse)
def chat(body: ChatRequest):
    if client is None:
        raise HTTPException(status_code=503, detail="AI backend is not configured")
    try:
        result = client.responses.create(
            model=MODEL,
            instructions=(
                "You are FRIDAY, a helpful personal AI assistant created by RR Digital Forge. "
                "Be capable, honest, and concise. Reply in the user's language. "
                "Support Marathi, Hindi, and English; follow the language of the user's message. "
                "Do not claim to have performed device or social-media actions unless a connected tool confirms it."
            ),
            input=body.message,
        )
        return ChatResponse(reply=result.output_text)
    except Exception:
        raise HTTPException(status_code=502, detail="AI service request failed")
