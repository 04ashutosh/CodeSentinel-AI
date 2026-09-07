from fastapi import FastAPI
import threading
import time
import requests
import socket
from app.config import settings

app = FastAPI(title=settings.APP_NAME)

# ==============================================================
# Eureka Registration Logic
# ==============================================================
def register_with_eureka():
    """
    Registers this Python FastAPI service with the Java Spring Cloud Eureka server.
    This allows the API Gateway to route traffic here natively.
    """
    hostname = socket.gethostname()
    ip_address = socket.gethostbyname(hostname)
    
    app_name = "GRAPH-INTELLIGENCE-SERVICE"
    port = 8091

    instance_payload = {
        "instance": {
            "instanceId": f"{hostname}:{app_name}:{port}",
            "hostName": ip_address,
            "app": app_name,
            "ipAddr": ip_address,
            "status": "UP",
            "port": {"$": port, "@enabled": "true"},
            "dataCenterInfo": {
                "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                "name": "MyOwn"
            }
        }
    }

    url = f"{settings.EUREKA_SERVER}apps/{app_name}"

    # Keep trying to register until Eureka is online
    while True:
        try:
            response = requests.post(url, json=instance_payload, headers={'Content-Type': 'application/json'})
            if response.status_code in [204, 200]:
                print(f"Successfully registered {app_name} with Eureka!")
                
                # Start sending heartbeats every 30 seconds
                while True:
                    time.sleep(30)
                    requests.put(f"{url}/{hostname}:{app_name}:{port}")
            else:
                print(f"Failed to register with Eureka. Status: {response.status_code}")
        except Exception as e:
            print(f"Waiting for Eureka server... {e}")
        time.sleep(5)

# Run Eureka registration in a background thread so it doesn't block FastAPI
@app.on_event("startup")
def startup_event():
    eureka_thread = threading.Thread(target=register_with_eureka, daemon=True)
    eureka_thread.start()

# ==============================================================
# Routes
# ==============================================================
@app.get("/api/v1/graph/health")
def health_check():
    return {"status": "UP", "service": settings.APP_NAME}

@app.get("/mcp/tools")
def get_mcp_tools():
    """
    Model Context Protocol (MCP) tool discovery endpoint.
    The AI Analysis service will hit this to discover what this service can do.
    """
    return {
        "tools": [
            {
                "name": "get_dependency_graph",
                "description": "Returns the structural dependency graph for a parsed project.",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "projectId": {"type": "string"}
                    }
                }
            }
        ]
    }