# 1. Use Python 3.10
FROM python:3.10-slim

# 2. Set working directory
WORKDIR /code

# 3. Install system dependencies (needed for Pillow/Image processing)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 4. Copy requirements and install
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. Copy the 'app' folder into the container
COPY ./app ./app

# 6. Run the app
# Note: We run from /code, so we call "app.main:app"
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
