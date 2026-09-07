from app.services.neo4j_service import neo4j_db

class GraphBuilderService:
    
    def build_project_graph(self, project_id: str, parsed_classes: list):
        """
        Takes a list of class metadata dictionaries (from our Java Parser Service)
        and builds a complete structural graph in Neo4j.
        """
        print(f"Building graph for project {project_id} with {len(parsed_classes)} classes...")

        # 1. Ensure the root Project node exists.
        # MERGE creates the node if it doesn't exist, or just returns it if it does.
        neo4j_db.execute_write(
            "MERGE (p:Project {projectId: $projectId})",
            {"projectId": str(project_id)}
        )

        # 2. Iterate through every Java class our parser found
        for cls in parsed_classes:
            class_name = cls.get("className")
            package_name = cls.get("packageName", "default")
            class_type = cls.get("classType", "UNKNOWN")
            
            # 3. Create the Package node and link it to the Project
            # MERGE the package so we don't create duplicates (e.g., if 5 classes share the same package)
            # Then MERGE a 'CONTAINS' relationship from Project to Package
            neo4j_db.execute_write(
                """
                MATCH (proj:Project {projectId: $projectId})
                MERGE (pkg:Package {name: $packageName, projectId: $projectId})
                MERGE (proj)-[:CONTAINS]->(pkg)
                """,
                {"projectId": str(project_id), "packageName": package_name}
            )

            # 4. Create the Class node itself
            # We save useful metadata like method counts and class types right onto the node
            neo4j_db.execute_write(
                """
                MATCH (pkg:Package {name: $packageName, projectId: $projectId})
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

            # 5. Handle Inheritance (Extends)
            # If our Java Parser noticed this class extends another (e.g., extends BaseEvent),
            # we draw an 'EXTENDS' arrow pointing to the parent class.
            extends_class = cls.get("extendsClass")
            if extends_class:
                neo4j_db.execute_write(
                    """
                    MATCH (child:Class {name: $childName, projectId: $projectId})
                    MERGE (parent:Class {name: $parentName, projectId: $projectId})
                    MERGE (child)-[:EXTENDS]->(parent)
                    """,
                    {
                        "projectId": str(project_id),
                        "childName": class_name,
                        "parentName": extends_class
                    }
                )

            # 6. Handle Interfaces (Implements)
            # If the class implements interfaces, loop through them and draw 'IMPLEMENTS' arrows.
            interfaces = cls.get("implementsInterfaces", [])
            for interface_name in interfaces:
                neo4j_db.execute_write(
                    """
                    MATCH (child:Class {name: $childName, projectId: $projectId})
                    MERGE (interface:Class {name: $interfaceName, projectId: $projectId})
                    SET interface.type = 'INTERFACE'
                    MERGE (child)-[:IMPLEMENTS]->(interface)
                    """,
                    {
                        "projectId": str(project_id),
                        "childName": class_name,
                        "interfaceName": interface_name
                    }
                )

        print(f"Successfully built graph for project {project_id}!")
        return True

# Singleton instance
graph_builder = GraphBuilderService()