import os
import time
import requests
import pypdf
from io import BytesIO

BASE_URL = "http://127.0.0.1:8000"

def create_sample_pdf(filename="sample_retrieval_doc.pdf"):
    from pypdf import PdfWriter
    writer = PdfWriter()
    
    # Page 1
    page1_text = (
        "Retrieval Augmented Generation (RAG) is a technique that enhances large language models "
        "by fetching relevant information from external knowledge bases. "
        "In RAG architectures, text documents are chunked into smaller passages and encoded into "
        "dense vector embeddings using Sentence Transformers such as all-MiniLM-L6-v2."
    )
    # Page 2
    page2_text = (
        "FAISS (Facebook AI Similarity Search) is a library for efficient similarity search "
        "and clustering of dense vectors. It contains algorithms that search in sets of vectors of any size, "
        "up to ones that possibly do not fit in RAM. FAISS uses inner product or Euclidean distance "
        "to return the most relevant vector indices."
    )
    # Page 3
    page3_text = (
        "FastAPI provides high-performance asynchronous API endpoints for Python applications. "
        "Retrofit is a type-safe HTTP client for Android and Java that allows seamless API calls "
        "to FastAPI backends using JSON converters like Gson."
    )
    
    for text in [page1_text, page2_text, page3_text]:
        page = writer.add_blank_page(width=612, height=792)
        # We can add text using reportlab or write directly if pdf generator available
    
    # Let's use reportlab or simple pypdf text creation if possible, or build simple PDF using reportlab or fpdf
    # If reportlab not installed, we can install reportlab or construct a standard minimal PDF
    return filename

if __name__ == "__main__":
    print("Testing backend server health...")
    try:
        r = requests.get(f"{BASE_URL}/health")
        print("Health Check Response:", r.status_code, r.json())
    except Exception as e:
        print("Error connecting to server:", e)
