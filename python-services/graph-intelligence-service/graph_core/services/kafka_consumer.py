from confluent_kafka import Consumer, KafkaError
import json
import requests
from django.conf import settings
from graph_core.services.graph_builder import graph_builder
import logging

logger = logging.getLogger(__name__)

class KafkaConsumerService:
    def __init__(self):
        self.conf = {
            'bootstrap.servers': settings.KAFKA_BOOTSTRAP_SERVERS,
            'group.id': 'django-graph-intelligence-group',
            'auto.offset.reset': 'earliest'
        }
        self.consumer = Consumer(self.conf)
        self.running = False

    def start_listening(self):
        self.consumer.subscribe(['graph.update', 'parsing.completed'])
        self.running = True
        logger.info("Kafka Consumer started. Subscribed to 'graph.update' and 'parsing.completed'...")

        while self.running:
            msg = self.consumer.poll(timeout=1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() != KafkaError._PARTITION_EOF:
                    logger.error(f"Kafka Error: {msg.error()}")
                continue

            try:
                topic = msg.topic()
                event = json.loads(msg.value().decode('utf-8'))
                project_id = event.get("projectId")
                if not project_id:
                    continue

                if topic == 'graph.update':
                    # Fetch multi-language UnifiedGraphPayload from parser-service
                    url = f"{settings.GATEWAY_URL}/api/v1/parser/projects/{project_id}/graph"
                    resp = requests.get(url, timeout=15)
                    if resp.status_code == 200:
                        payload = resp.json().get("data", {})
                        graph_builder.build_unified_graph(project_id, payload)
                elif topic == 'parsing.completed':
                    # Legacy fallback
                    url = f"{settings.GATEWAY_URL}/api/v1/parser/projects/{project_id}/classes"
                    resp = requests.get(url, timeout=15)
                    if resp.status_code == 200:
                        classes = resp.json().get("data", [])
                        graph_builder.build_project_graph(project_id, classes)
            except Exception as e:
                logger.error(f"Failed to process Kafka message: {e}")

    def stop(self):
        self.running = False
        self.consumer.close()

kafka_consumer_service = KafkaConsumerService()