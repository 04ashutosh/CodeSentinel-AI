from django.apps import AppConfig
import threading
import os

class GraphCoreConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'graph_core'

    def ready(self):
        # Auto-start Kafka consumer background thread only when running as server
        if os.environ.get('RUN_MAIN') != 'true' and os.environ.get('START_KAFKA_CONSUMER', 'true').lower() == 'true':
            from graph_core.services.kafka_consumer import kafka_consumer_service
            consumer_thread = threading.Thread(target=kafka_consumer_service.start_listening, daemon=True)
            consumer_thread.start()