from ninja import NinjaAPI, Schema
from chat_agent.services.agent import code_sentinel_agent
from typing import Optional

api = NinjaAPI(title="CodeSentinel AI Analysis API", version="2.0.0")

class ChatRequest(Schema):
    question: str
    projectId: Optional[str] = None

class ChatResponse(Schema):
    response: str

@api.post("/api/v1/ai/chat", response=ChatResponse)
def chat(request, payload: ChatRequest):
    answer = code_sentinel_agent.ask(payload.question, payload.projectId)
    return {"response": answer}