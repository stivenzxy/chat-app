FROM python:3.9-slim

# Instalar dependencias del sistema
RUN apt-get update && apt-get install -y \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Instalar VOSK y dependencias
RUN pip install --no-cache-dir vosk websockets

# Crear directorio para el modelo
RUN mkdir -p /opt/vosk-model

# Copiar el servidor WebSocket
COPY asr_server.py /opt/asr_server.py

# Exponer puerto
EXPOSE 2700

# Variable de entorno para el modelo
ENV MODEL_PATH=/opt/vosk-model

# Comando para ejecutar el servidor
CMD ["python", "/opt/asr_server.py", "/opt/vosk-model"]
