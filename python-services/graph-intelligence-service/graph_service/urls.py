from django.urls import path
from graph_core.api import api

urlpatterns = [
    path("", api.urls),
]