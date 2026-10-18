from ninja import NinjaAPI
from graph_core.services.neo4j_service import neo4j_db
from graph_core.services.graph_builder import graph_builder
from typing import List, Dict, Any

api = NinjaAPI(title="CodeSentinel Graph Intelligence API", version="2.0.0")

@api.get("/api/v1/graph/projects/{project_id}/nodes")
def get_graph_nodes(request, project_id: str):
    query = """
    MATCH (n)
    WHERE n.projectId = $projectId
    RETURN n
    """
    results = neo4j_db.execute_read(query, {"projectId": str(project_id)})
    nodes = [record["n"] for record in results]
    return {"status": "success", "data": nodes}

@api.get("/api/v1/graph/projects/{project_id}/edges")
def get_graph_edges(request, project_id: str):
    query = """
    MATCH (source)-[r]->(target)
    WHERE source.projectId = $projectId AND target.projectId = $projectId
    RETURN source.name as source, type(r) as relationship, target.name as target,
           r.confidence as confidence, r.verificationStatus as verificationStatus
    """
    results = neo4j_db.execute_read(query, {"projectId": str(project_id)})
    return {"status": "success", "data": results}

@api.post("/api/v1/graph/projects/{project_id}/build")
def build_graph(request, project_id: str, payload: Dict[str, Any]):
    success = graph_builder.build_unified_graph(project_id, payload)
    return {"status": "success" if success else "failed"}