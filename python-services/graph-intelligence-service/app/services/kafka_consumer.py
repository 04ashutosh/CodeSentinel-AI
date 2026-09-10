from confluent_kafka import Consumer, KafkaError
import json
import threading
import requests
from app.config import settings
from app.services.graph_builder import graph_builder

class KafkaConsumerService:
    def __init__(self):
        self.conf = {
            'bootstrap.servers': settings.KAFKA_BOOTSTRAP_SERVERS,
            'group.id': 'graph-intelligence-group',
            'auto.offset.reset': 'earliest'
        }
        self.consumer = Consumer(self.conf)
        self.running = False

    def start_listening(self):
        """Starts the Kafka consumer loop in a background thread."""
        self.consumer.subscribe(['parsing.completed'])
        self.running = True
        
        print("Kafka Consumer started. Listening for 'parsing.completed'...")
        
        while self.running:
            # Poll for messages every 1 second
            msg = self.consumer.poll(timeout=1.0)
            
            if msg is None:
                continue
                
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    print(f"Kafka Error: {msg.error()}")
                    continue

            # Process the message
            try:
                # 1. Decode the Kafka message payload
                raw_value = msg.value().decode('utf-8')
                event = json.loads(raw_value)
                
                project_id = event.get("projectId")
                
                if project_id:
                    print(f"Received parsing.completed for Project ID: {project_id}")
                    
                    # 2. Fetch the parsed Java metadata from our Java API Gateway
                    # We hit the Parser Service to get all classes for this project
                    api_url = f"http://gateway-service:8080/api/v1/parser/projects/{project_id}/classes"
                    response = requests.get(api_url)
                    
                    if response.status_code == 200:
                        data = response.json()
                        parsed_classes = data.get("data", [])
                        
                        # 3. Pipe the JSON into Neo4j!
                        graph_builder.build_project_graph(project_id, parsed_classes)
                    else:
                        print(f"Failed to fetch classes from parser service. HTTP {response.status_code}")
            except Exception as e:
                print(f"Failed to process Kafka message: {e}")

    def stop(self):
        self.running = False
        self.consumer.close()

# Singleton instance
kafka_consumer_service = KafkaConsumerService()