import os
from pydantic import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "CodeSentinel AI Analysis Service"
    EUREKA_SERVER: str = os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka/")
    OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    GATEWAY_URL: str = os.getenv("GATEWAY_URL", "http://localhost:8080")
    GRAPH_SERVICE_URL: str = os.getenv("GRAPH_SERVICE_URL", "http://localhost:8091")

    class Config:
        env_file = ".env"

settings = Settings()
