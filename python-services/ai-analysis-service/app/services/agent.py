from langchain_community.chat_models import ChatOllama
from langchain.schema import HumanMessage, SystemMessage
from app.config import settings
from app.services.mcp_client import mcp_client

class CodeSentinelAgent:
    def __init__(self):
        self.llm = ChatOllama(
            base_url=settings.OLLAMA_BASE_URL,
            model="llama3",
            temperature=0.1
        )
        
        # Get the graph tool from MCP client
        self.tools = mcp_client.fetch_graph_tools()
        self.graph_tool = self.tools[0] if self.tools else None

    def ask(self, question: str, project_id: str = None) -> str:
        """
        Simple RAG approach:
        1. Always fetch the graph data for the project
        2. Pass it as context to the LLM along with the user's question
        """
        graph_context = ""
        
        if project_id and self.graph_tool:
            try:
                print(f"Fetching graph data for project: {project_id}")
                graph_context = self.graph_tool.run(project_id)
                print(f"Graph data fetched successfully ({len(graph_context)} chars)")
            except Exception as e:
                print(f"Error fetching graph: {e}")
                graph_context = f"Error fetching graph data: {str(e)}"

        messages = [
            SystemMessage(content=f"""You are CodeSentinel AI, an expert code analysis assistant.
You have access to the following project dependency graph data. Use it to answer the user's question accurately.
If the data doesn't contain enough information, say so honestly.

PROJECT GRAPH DATA:
{graph_context}
"""),
            HumanMessage(content=question)
        ]
        
        try:
            response = self.llm.invoke(messages)
            return response.content
        except Exception as e:
            return f"Agent encountered an error: {str(e)}"

# Singleton instance
code_sentinel_agent = CodeSentinelAgent()