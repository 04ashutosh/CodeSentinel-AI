from fastapi import APIRouter
from pydantic import BaseModel
from app.services.agent import code_sentinel_agent

router = APIRouter(prefix="/api/v1/ai", tags=["AI Analysis"])

class ChatRequest(BaseModel):
    projectId: str
    question: str

@router.post("/chat")
def chat_with_agent(request: ChatRequest):
    """
    Endpoint for the Angular frontend to chat with the LLM about a specific project.
    """
    answer = code_sentinel_agent.ask(request.question, request.projectId)
    return {"status": "success", "answer": answer}
