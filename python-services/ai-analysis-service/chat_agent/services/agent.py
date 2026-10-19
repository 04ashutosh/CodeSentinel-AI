from langchain_community.chat_models import ChatOllama
from langchain.schema import HumanMessage, SystemMessage
from django.conf import settings
from chat_agent.services.mcp_client import mcp_client

class CodeSentinelAgent:
    def __init__(self):
        self.llm = ChatOllama(
            base_url=settings.OLLAMA_BASE_URL,
            model="llama3",
            temperature=0.1
        )
        self.tools = mcp_client.fetch_graph_tools()
        self.graph_tool = self.tools[0] if self.tools else None

    def ask(self, question: str, project_id: str = None) -> str:
        graph_context = ""
        if project_id and self.graph_tool:
            try:
                graph_context = self.graph_tool.run(project_id)
            except Exception as e:
                graph_context = f"Error fetching graph: {e}"

        messages = [
            SystemMessage(content=f"""You are CodeSentinel AI, an advanced multi-language code analysis assistant.
Use the following project graph data to answer the user's questions about codebase architecture, files, functions, and relationships.

UNIVERSAL PROJECT GRAPH:
{graph_context}
"""),
            HumanMessage(content=question)
        ]
        try:
            return self.llm.invoke(messages).content
        except Exception as e:
            return f"Agent error: {str(e)}"

code_sentinel_agent = CodeSentinelAgent()