from django.urls import path
from chat_agent.api import api

urlpatterns = [
    path("", api.urls),
]