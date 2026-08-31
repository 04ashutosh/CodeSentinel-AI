from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "Graph Intelligence Service"
    NEO4J_URI: str = "bolt://neo4j:7687"
    NEO4J_USER: str = "neo4j"
    NEO4J_PASSWORD: str = "codesentinel_neo4j_2024"
    KAFKA_BOOTSTRAP_SERVERS: str = "kafka:9092"
    EUREKA_SERVER: str = "http://discovery-server:8761/eureka/"

    class Config:
        env_file = ".env"

settings = Settings()