# 1. Start with a lightweight Python image
FROM docker.io/library/python:3.12-slim

# 2. Set the working directory inside the container
WORKDIR /app

# 3. Copy ONLY the requirements file first.
# Just like Maven, this caches pip installations so dont need to 
# re-download packages every time change a single line of Python code!
COPY requirements.txt .

# 4. Install the Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# 5. Copy application code into the container
COPY . .

# 6. Expose the port FastAPI server runs on
EXPOSE 8000

# 7. Start the FastAPI server using the command from your README
CMD ["python", "-m", "app.main"]