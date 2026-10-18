from graph_core.services.neo4j_service import neo4j_db
import logging

logger = logging.getLogger(__name__)

class GraphBuilderService:

    def build_unified_graph(self, project_id: str, payload: dict):
        """
        Ingests the Universal Intermediate Representation (UIR) payload into Neo4j.
        Supports any programming language without hardcoding schemas.
        """
        nodes = payload.get("nodes", [])
        relationships = payload.get("relationships", [])
        logger.info(f"Building universal graph for project {project_id}: {len(nodes)} nodes, {len(relationships)} edges")

        # 1. Project Root Node
        neo4j_db.execute_write(
            "MERGE (p:Project {projectId: $projectId})",
            {"projectId": str(project_id)}
        )

        # 2. Persist Unified Nodes
        for node in nodes:
            node_id = node.get("id")
            category = node.get("category", "CODE_ELEMENT").capitalize()
            lang = node.get("language", "unknown")
            name = node.get("name", "")
            qname = node.get("qualifiedName", name)
            tier = node.get("extractionTier", "TIER_3_GENERIC")
            confidence = node.get("confidence", 1.0)
            status = node.get("verificationStatus", "VERIFIED_FACT")
            file_id = node.get("fileId", "")

            # Idempotently merge node with label :CodeElement and secondary dynamic label (e.g., :Class, :Function, :File)
            query = f"""
            MERGE (n:CodeElement {{id: $nodeId}})
            SET n:{category},
                n.projectId = $projectId,
                n.name = $name,
                n.qualifiedName = $qname,
                n.language = $language,
                n.category = $category,
                n.extractionTier = $tier,
                n.confidence = $confidence,
                n.verificationStatus = $status,
                n.filePath = $fileId
            """
            neo4j_db.execute_write(query, {
                "nodeId": node_id,
                "projectId": str(project_id),
                "name": name,
                "qname": qname,
                "language": lang,
                "category": category,
                "tier": tier,
                "confidence": confidence,
                "status": status,
                "fileId": file_id
            })

            # Also maintain backward-compatible Package and Class labels for Java queries
            if category == "Package":
                neo4j_db.execute_write(
                    "MATCH (n:CodeElement {id: $nodeId}) SET n:Package",
                    {"nodeId": node_id}
                )
            elif category in ("Class", "Interface"):
                neo4j_db.execute_write(
                    "MATCH (n:CodeElement {id: $nodeId}) SET n:Class, n.type = $cat",
                    {"nodeId": node_id, "cat": category.upper()}
                )

        # 3. Persist Unified Relationships
        for rel in relationships:
            source_id = rel.get("sourceId")
            target_id = rel.get("targetId")
            rel_type = rel.get("type", "REFERENCES").upper()
            confidence = rel.get("confidence", 1.0)
            status = rel.get("verificationStatus", "VERIFIED_FACT")

            # Dynamic relationship merge
            query = f"""
            MATCH (s:CodeElement {{id: $sourceId}})
            MATCH (t:CodeElement {{id: $targetId}})
            MERGE (s)-[r:{rel_type}]->(t)
            SET r.confidence = $confidence,
                r.verificationStatus = $status,
                r.projectId = $projectId
            """
            neo4j_db.execute_write(query, {
                "sourceId": source_id,
                "targetId": target_id,
                "confidence": confidence,
                "status": status,
                "projectId": str(project_id)
            })

        logger.info(f"Successfully built universal graph for project {project_id}!")
        return True

    def build_project_graph(self, project_id: str, parsed_classes: list):
        """Legacy Java backward-compatible graph builder"""
        neo4j_db.execute_write(
            "MERGE (p:Project {projectId: $projectId})",
            {"projectId": str(project_id)}
        )
        for cls in parsed_classes:
            class_name = cls.get("className")
            package_name = cls.get("packageName", "default")
            class_type = cls.get("classType", "UNKNOWN")

            neo4j_db.execute_write(
                """
                MATCH (proj:Project {projectId: $projectId})
                MERGE (pkg:Package {name: $packageName, projectId: $projectId})
                MERGE (proj)-[:CONTAINS]->(pkg)
                MERGE (c:Class {name: $className, projectId: $projectId})
                SET c.type = $classType,
                    c.methodCount = $methodCount,
                    c.fieldCount = $fieldCount,
                    c.filePath = $filePath
                MERGE (pkg)-[:CONTAINS]->(c)
                """,
                {
                    "projectId": str(project_id),
                    "packageName": package_name,
                    "className": class_name,
                    "classType": class_type,
                    "methodCount": cls.get("methodCount", 0),
                    "fieldCount": cls.get("fieldCount", 0),
                    "filePath": cls.get("filePath", "")
                }
            )
        return True

graph_builder = GraphBuilderService()