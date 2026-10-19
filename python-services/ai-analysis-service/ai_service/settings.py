import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

SECRET_KEY = os.environ.get('SECRET_KEY', 'django-insecure-codesentinel-ai-analysis-key')
DEBUG = os.environ.get('DEBUG', 'False').lower() in ('true', '1')
ALLOWED_HOSTS = ['*']

INSTALLED_APPS = [
    'django.contrib.contenttypes',
    'django.contrib.staticfiles',
    'chat_agent.apps.ChatAgentConfig',
]

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'django.middleware.common.CommonMiddleware',
]

ROOT_URLCONF = 'ai_service.urls'
WSGI_APPLICATION = 'ai_service.wsgi.application'
ASGI_APPLICATION = 'ai_service.asgi.application'

DATABASES = {}

LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'UTC'
USE_I18N = True
USE_TZ = True
STATIC_URL = 'static/'

OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://host.docker.internal:11434")
GRAPH_SERVICE_URL = os.environ.get("GRAPH_SERVICE_URL", "http://graph-intelligence-service:8091")