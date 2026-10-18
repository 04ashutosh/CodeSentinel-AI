from neo4j import GraphDatabase
from django.conf import settings
import logging

logger = logging.getLogger(__name__)

class Neo4jService:
    def __init__(self):
        self.driver = None

    def get_driver(self):
        if not self.driver:
            self.driver = GraphDatabase.driver(
                settings.NEO4J_URI,
                auth=(settings.NEO4J_USER, settings.NEO4J_PASSWORD)
            )
        return self.driver

    def close(self):
        if self.driver:
            self.driver.close()

    def execute_write(self, query: str, parameters: dict = None):
        driver = self.get_driver()
        with driver.session() as session:
            return session.execute_write(lambda tx: tx.run(query, parameters or {}).data())

    def execute_read(self, query: str, parameters: dict = None):
        driver = self.get_driver()
        with driver.session() as session:
            return session.execute_read(lambda tx: tx.run(query, parameters or {}).data())

neo4j_db = Neo4jService()