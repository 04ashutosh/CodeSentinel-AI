from neo4j import GraphDatabase
from app.config import settings

class Neo4jService:
    def __init__(self):
        # Initialize connection to Neo4j using credentials from config.py
        self.driver = GraphDatabase.driver(
            settings.NEO4J_URI, 
            auth=(settings.NEO4J_USER, settings.NEO4J_PASSWORD)
        )

    def close(self):
        self.driver.close()

    def clear_database(self):
        """Wipes the entire Neo4j database. Useful for clean starts."""
        with self.driver.session() as session:
            session.run("MATCH (n) DETACH DELETE n")
            print("Neo4j Database cleared!")

    def execute_write(self, query: str, parameters: dict = None):
        """Helper method to run a write transaction in Neo4j."""
        with self.driver.session() as session:
            return session.write_transaction(self._do_execute, query, parameters)

    def execute_read(self, query: str, parameters: dict = None):
        """Helper method to run a read transaction in Neo4j."""
        with self.driver.session() as session:
            return session.read_transaction(self._do_execute, query, parameters)

    @staticmethod
    def _do_execute(tx, query, parameters):
        result = tx.run(query, parameters)
        return [record.data() for record in result]

# Create a singleton instance we can import across our app
neo4j_db = Neo4jService()