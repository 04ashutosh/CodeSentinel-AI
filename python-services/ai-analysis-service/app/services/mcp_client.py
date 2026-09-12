import requests
from typing import List, Dict, Any
from langchain.tools import StructuredTool
from app.config import settings

class McpClientService:
    def __init__(self):
        # We route our requests through the API Gateway!
        self.graph_service_url = f"{settings.GATEWAY_URL}/api/v1/graph"

    def fetch_graph_tools(self) -> List[StructuredTool]:
        tools = []
        
        def execute_get_dependency_graph(projectId: str) -> str:
            print(f"Agent requested graph for project: {projectId}")
            
            try:
                # 1. Fetch Nodes
                nodes_response = requests.get(f"{self.graph_service_url}/projects/{projectId}/nodes", timeout=10)
                nodes_data = nodes_response.json().get("data", [])
                
                # 2. Fetch Edges
                edges_response = requests.get(f"{self.graph_service_url}/projects/{projectId}/edges", timeout=10)
                edges_data = edges_response.json().get("data", [])
                
                # Format a clean string representation for the LLM
                summary = f"Project {projectId} Graph:\n\nNodes:\n"
                for node in nodes_data:
                    summary += f"- {node.get('type', 'UNKNOWN')}: {node.get('name')} (Package: {node.get('packageName')})\n"
                    
                summary += "\nRelationships:\n"
                for edge in edges_data:
                    summary += f"- {edge.get('source')} -> {edge.get('relationship')} -> {edge.get('target')}\n"
                    
                return summary
            except Exception as e:
                return f"Error executing tool: {str(e)}"

        # Wrap it into a LangChain StructuredTool without explicitly defining args_schema.
        # LangChain will infer the schema from the python type hints (projectId: str).
        graph_tool = StructuredTool.from_function(
            func=execute_get_dependency_graph,
            name="get_dependency_graph",
            description="Returns the structural dependency graph for a parsed project, including all classes, interfaces, and their relationships (EXTENDS, IMPLEMENTS, CONTAINS)."
        )
        
        tools.append(graph_tool)
        return tools

# Singleton instance
mcp_client = McpClientService()
