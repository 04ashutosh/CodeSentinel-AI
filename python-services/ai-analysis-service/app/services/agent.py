from langchain_community.chat_models import ChatOllama
from langchain.agents import initialize_agent, AgentType
from langchain.memory import ConversationBufferWindowMemory
from app.config import settings
from app.services.mcp_client import mcp_client

class CodeSentinelAgent:
    def __init__(self):
        # We connect to your host machine's Ollama instance!
        # Make sure you have pulled a model e.g. `ollama run llama3`
        self.llm = ChatOllama(
            base_url=settings.OLLAMA_BASE_URL,
            model="llama3",
            temperature=0.1
        )
        
        # Get the tools discovered via MCP
        self.tools = mcp_client.fetch_graph_tools()
        
        # Give the agent a memory window so it can remember context in a conversation
        self.memory = ConversationBufferWindowMemory(
            memory_key="chat_history",
            k=5,
            return_messages=True
        )
        
        # Initialize the LangChain Agent
        self.agent = initialize_agent(
            tools=self.tools,
            llm=self.llm,
            agent=AgentType.CHAT_CONVERSATIONAL_REACT_DESCRIPTION,
            verbose=True,
            memory=self.memory,
            handle_parsing_errors=True
        )

    def ask(self, question: str, project_id: str = None) -> str:
        """
        Sends a query to the agent. We prepend the project_id to the prompt so 
        the agent knows which project it is currently assisting with.
        """
        system_context = ""
        if project_id:
            system_context = f"Context: The user is currently asking about Project ID: {project_id}. Use your tools to fetch data about this project if needed.\n\n"
            
        full_prompt = system_context + question
        
        try:
            response = self.agent.run(full_prompt)
            return response
        except Exception as e:
            return f"Agent encountered an error: {str(e)}"

# Singleton instance
code_sentinel_agent = CodeSentinelAgent()