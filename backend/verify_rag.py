import os
import sys
import time
import requests
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

SERVER_URL = "http://127.0.0.1:8000"
PDF_FILE_PATH = "sample_retrieval_lab_doc.pdf"

def generate_sample_pdf(filename=PDF_FILE_PATH):
    print(f"Creating sample PDF document '{filename}'...")
    c = canvas.Canvas(filename, pagesize=letter)
    
    # Page 1
    c.setFont("Helvetica-Bold", 16)
    c.drawString(72, 720, "Retrieval System Lab: RAG Architecture Overview")
    c.setFont("Helvetica", 12)
    c.drawString(72, 680, "Retrieval Augmented Generation (RAG) integrates vector search with language models.")
    c.drawString(72, 660, "Text documents are chunked into smaller passages, typically 300 to 500 characters long.")
    c.drawString(72, 640, "Each text chunk is converted into a 384-dimensional vector embedding using all-MiniLM-L6-v2.")
    c.showPage()
    
    # Page 2
    c.setFont("Helvetica-Bold", 16)
    c.drawString(72, 720, "FAISS Index & Vector Search")
    c.setFont("Helvetica", 12)
    c.drawString(72, 680, "FAISS (Facebook AI Similarity Search) is an open-source vector store designed for fast retrieval.")
    c.drawString(72, 660, "Inner Product (IndexFlatIP) combined with L2 normalized embeddings calculates Cosine Similarity.")
    c.drawString(72, 640, "FAISS stores vector indices persistently on disk alongside chunk metadata for instant search.")
    c.showPage()
    
    # Page 3
    c.setFont("Helvetica-Bold", 16)
    c.drawString(72, 720, "Android Kotlin & FastAPI Integration")
    c.setFont("Helvetica", 12)
    c.drawString(72, 680, "The Android app uses Retrofit 2 with OkHttp to connect to the FastAPI backend at 10.0.2.2:8000.")
    c.drawString(72, 660, "Android users upload PDF files via multipart/form-data and query the index with top_k = 3.")
    c.drawString(72, 640, "The user interface follows a clean White and Violet Material 3 design system.")
    c.showPage()
    
    c.save()
    print("Sample PDF generated successfully.")

def test_api():
    print("\n--- 1. Testing GET /health ---")
    res = requests.get(f"{SERVER_URL}/health")
    print("Health Status:", res.status_code, res.json())
    
    print("\n--- 2. Testing POST /documents/upload ---")
    with open(PDF_FILE_PATH, "rb") as f:
        files = {"file": (PDF_FILE_PATH, f, "application/pdf")}
        upload_res = requests.post(f"{SERVER_URL}/documents/upload", files=files)
    print("Upload Status Code:", upload_res.status_code)
    print("Upload Response JSON:", upload_res.json())
    
    print("\n--- 3. Testing POST /search/ (Question 1) ---")
    query1 = {"query": "What embedding model and vector dimension are used?", "top_k": 3}
    search_res1 = requests.post(f"{SERVER_URL}/search/", json=query1)
    print("Query 1 Search Code:", search_res1.status_code)
    print("Query 1 Results JSON:")
    print(search_res1.json())
    
    print("\n--- 4. Testing POST /search/ (Question 2) ---")
    query2 = {"query": "How does FAISS calculate similarity?", "top_k": 3}
    search_res2 = requests.post(f"{SERVER_URL}/search/", json=query2)
    print("Query 2 Search Code:", search_res2.status_code)
    print("Query 2 Results JSON:")
    print(search_res2.json())

if __name__ == "__main__":
    if not os.path.exists(PDF_FILE_PATH):
        generate_sample_pdf()
    test_api()
