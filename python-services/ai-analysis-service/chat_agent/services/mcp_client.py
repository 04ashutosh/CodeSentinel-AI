import requests
from typing import List, Dict, Any
try:
    from langchain_core.tools import StructuredTool
except ImportError:
    from langchain.tools import StructuredTool
from django.conf import settings

class McpClientService:
    def __init__(self):
        self.graph_service_url = f"{settings.GRAPH_SERVICE_URL}/api/v1/graph"

    def fetch_graph_tools(self) -> List[StructuredTool]:
        def execute_get_dependency_graph(projectId: str) -> str:
            try:
                # 1. Fetch Nodes & Edges from Django Graph Intelligence Service
                nodes_res = requests.get(f"{self.graph_service_url}/projects/{projectId}/nodes", timeout=10)
                nodes_data = nodes_res.json().get("data", [])
                
                edges_res = requests.get(f"{self.graph_service_url}/projects/{projectId}/edges", timeout=10)
                edges_data = edges_res.json().get("data", [])
                
                summary = f"Project {projectId} Universal Codebase Structure:\n"

                # Group by language & files
                by_lang = {}
                for node in nodes_data:
                    lang = node.get('language', 'generic')
                    by_lang.setdefault(lang, []).append(node)

                for lang, lang_nodes in by_lang.items():
                    summary += f"\n--- Language: {lang.upper()} ({len(lang_nodes)} elements) ---\n"
                    # Prioritize architectural elements (Class, File, Interface, Service, Controller, etc.)
                    arch_nodes = [n for n in lang_nodes if n.get('category') in ('Class', 'File', 'Interface', 'Service', 'Controller', 'Module')]
                    other_nodes = [n for n in lang_nodes if n not in arch_nodes]

                    for node in arch_nodes[:60]:
                        name = node.get('name', '')
                        cat = node.get('category', node.get('type', 'ELEMENT'))
                        file_path = node.get('filePath', '')
                        summary += f"  - [{cat}] {name} (path: {file_path})\n"

                    if other_nodes:
                        summary += f"  - ... and {len(other_nodes)} inner methods/functions/members\n"

                if edges_data:
                    summary += f"\nKey Relationships (Top {min(len(edges_data), 60)}):\n"
                    for edge in edges_data[:60]:
                        summary += f"  - {edge.get('source')} --[{edge.get('relationship')}]--> {edge.get('target')}\n"

                return summary
            except Exception as e:
                return f"Error executing tool: {str(e)}"

        return [StructuredTool.from_function(
            func=execute_get_dependency_graph,
            name="get_dependency_graph",
            description="Returns the universal multi-language code structure."
        )]

mcp_client = McpClientService()
