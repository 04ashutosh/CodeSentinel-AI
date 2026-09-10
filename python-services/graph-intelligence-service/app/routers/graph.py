from fastapi import APIRouter
from app.services.neo4j_service import neo4j_db

router = APIRouter(prefix="/api/v1/graph", tags=["Graph Intelligence"])

@router.get("/projects/{project_id}/nodes")
def get_graph_nodes(project_id: str):
    """
    Fetches all Classes and Packages for a specific project from Neo4j.
    Used by the Angular Frontend to draw the graph visually.
    """
    query = """
    MATCH (n)
    WHERE n.projectId = $projectId
    RETURN n
    """
    results = neo4j_db.execute_read(query, {"projectId": str(project_id)})
    
    # Clean up the output so it's easy for the frontend to consume
    nodes = [record["n"] for record in results]
    return {"status": "success", "data": nodes}


@router.get("/projects/{project_id}/edges")
def get_graph_edges(project_id: str):
    """
    Fetches all relationships (EXTENDS, IMPLEMENTS, CONTAINS) for a project.
    """
    query = """
    MATCH (source)-[r]->(target)
    WHERE source.projectId = $projectId AND target.projectId = $projectId
    RETURN source.name as source, type(r) as relationship, target.name as target
    """
    results = neo4j_db.execute_read(query, {"projectId": str(project_id)})
    
    return {"status": "success", "data": results}