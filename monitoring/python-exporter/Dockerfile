FROM python:3.12-slim

WORKDIR /app

# install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY exporter.py .

# run as non-root user
RUN useradd -m exporteruser
USER exporteruser

EXPOSE 8000

CMD ["python", "exporter.py"]
