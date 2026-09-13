import requests
from typing import List, Dict, Any
from langchain.tools import StructuredTool
from app.config import settings

class McpClientService:
    def __init__(self):
        # Connect directly to the graph-intelligence service (not through Gateway)
        self.graph_service_url = f"{settings.GRAPH_SERVICE_URL}/api/v1/graph"

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
                
                # Separate package nodes from class nodes
                current_package = "unknown"
                summary = f"Project {projectId} Code Structure:\n\n"
                
                for node in nodes_data:
                    name = node.get('name', '')
                    node_type = node.get('type')
                    file_path = node.get('filePath')
                    
                    if not node_type and not file_path and name:
                        # This is a package node
                        current_package = name
                        summary += f"\nPackage: {name}\n"
                    elif file_path:
                        # This is a class/interface node
                        kind = node_type or 'CLASS'
                        fields = node.get('fieldCount', 0)
                        methods = node.get('methodCount', 0)
                        summary += f"  - {kind}: {name} ({fields} fields, {methods} methods) [{file_path}]\n"
                    elif node_type and name:
                        # External reference (like RuntimeException)
                        summary += f"  - EXTERNAL {node_type}: {name}\n"
                
                if edges_data:
                    summary += "\nRelationships:\n"
                    for edge in edges_data:
                        summary += f"  - {edge.get('source')} --[{edge.get('relationship')}]--> {edge.get('target')}\n"
                    
                return summary
            except Exception as e:
                return f"Error executing tool: {str(e)}"

        graph_tool = StructuredTool.from_function(
            func=execute_get_dependency_graph,
            name="get_dependency_graph",
            description="Returns the structural dependency graph for a parsed project, including all classes, interfaces, and their relationships (EXTENDS, IMPLEMENTS, CONTAINS)."
        )
        
        tools.append(graph_tool)
        return tools

# Singleton instance
mcp_client = McpClientService()
