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
    hostname = socket.gethostname()
    ip_address = socket.gethostbyname(hostname)
    
    app_name = "AI-ANALYSIS-SERVICE"
    port = 8090

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

    while True:
        try:
            response = requests.post(url, json=instance_payload, headers={'Content-Type': 'application/json'})
            if response.status_code in [204, 200]:
                print(f"Successfully registered {app_name} with Eureka!")
                
                while True:
                    time.sleep(30)
                    requests.put(f"{url}/{hostname}:{app_name}:{port}")
            else:
                print(f"Failed to register with Eureka. Status: {response.status_code}")
        except Exception as e:
            print(f"Waiting for Eureka server... {e}")
        time.sleep(5)

@app.on_event("startup")
def startup_event():
    eureka_thread = threading.Thread(target=register_with_eureka, daemon=True)
    eureka_thread.start()

# ==============================================================
# Routes
# ==============================================================
from app.routers import chat
app.include_router(chat.router)
