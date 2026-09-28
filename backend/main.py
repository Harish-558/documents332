import os
import io
import json
import logging
from typing import List, Dict, Any
from contextlib import asynccontextmanager

from fastapi import FastAPI, UploadFile, File, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pypdf
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("RetrievalSystem")

# Global variables for model, index, and metadata
VECTOR_STORE_DIR = "vector_store"
INDEX_FILE = os.path.join(VECTOR_STORE_DIR, "index.faiss")
METADATA_FILE = os.path.join(VECTOR_STORE_DIR, "metadata.json")
EMBEDDING_DIM = 384  # Dimension for all-MiniLM-L6-v2

embedding_model: SentenceTransformer = None
faiss_index: faiss.IndexFlatIP = None
chunks_metadata: List[Dict[str, Any]] = []

def load_vector_store():
    global faiss_index, chunks_metadata
    os.makedirs(VECTOR_STORE_DIR, exist_ok=True)
    
    if os.path.exists(INDEX_FILE) and os.path.exists(METADATA_FILE):
        try:
            logger.info("Loading existing FAISS index and metadata...")
            faiss_index = faiss.read_index(INDEX_FILE)
            with open(METADATA_FILE, "r", encoding="utf-8") as f:
                chunks_metadata = json.load(f)
            logger.info(f"Loaded {faiss_index.ntotal} vectors and {len(chunks_metadata)} metadata entries.")
        except Exception as e:
            logger.error(f"Failed to load vector store: {e}. Reinitializing empty store.")
            faiss_index = faiss.IndexFlatIP(EMBEDDING_DIM)
            chunks_metadata = []
    else:
        logger.info("Creating new empty FAISS index...")
        faiss_index = faiss.IndexFlatIP(EMBEDDING_DIM)
        chunks_metadata = []

def save_vector_store():
    os.makedirs(VECTOR_STORE_DIR, exist_ok=True)
    if faiss_index is not None:
        faiss.write_index(faiss_index, INDEX_FILE)
    with open(METADATA_FILE, "w", encoding="utf-8") as f:
        json.dump(chunks_metadata, f, ensure_ascii=False, indent=2)
    logger.info("Vector store successfully saved to disk.")

@asynccontextmanager
async def lifespan(app: FastAPI):
    global embedding_model
    logger.info("Initializing SentenceTransformer model (all-MiniLM-L6-v2)...")
    embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
    load_vector_store()
    yield
    logger.info("Saving vector store before shutdown...")
    save_vector_store()

app = FastAPI(
    title="Retrieval System Lab API",
    description="PDF Document Chunking, Embedding with Sentence Transformers, FAISS Vector Search",
    version="1.0.0",
    lifespan=lifespan
)

# Allow CORS for all origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class SearchRequest(BaseModel):
    query: str
    top_k: int = 3

def chunk_text(text: str, chunk_size: int = 500, overlap: int = 100) -> List[str]:
    """Splits text into overlapping chunks."""
    cleaned = " ".join(text.split())
    if not cleaned:
        return []
    
    chunks = []
    start = 0
    text_len = len(cleaned)
    
    while start < text_len:
        end = start + chunk_size
        chunk = cleaned[start:end]
        
        # Avoid breaking words if possible
        if end < text_len:
            last_space = chunk.rfind(" ")
            if last_space > chunk_size // 2:
                chunk = chunk[:last_space]
                end = start + last_space
                
        chunks.append(chunk.strip())
        start = max(start + 1, end - overlap)
        
    return chunks

@app.get("/health")
def health_check():
    total_chunks = faiss_index.ntotal if faiss_index else 0
    unique_docs = len(set(meta["document"] for meta in chunks_metadata)) if chunks_metadata else 0
    return {
        "status": "ok",
        "total_chunks": total_chunks,
        "indexed_documents": unique_docs
    }

@app.post("/documents/upload")
async def upload_document(file: UploadFile = File(...)):
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only PDF files are supported."
        )
    
    try:
        contents = await file.read()
        pdf_reader = pypdf.PdfReader(io.BytesIO(contents))
        num_pages = len(pdf_reader.pages)
        
        new_chunks = []
        new_metadata = []
        
        for page_idx, page in enumerate(pdf_reader.pages):
            page_text = page.extract_text() or ""
            extracted_chunks = chunk_text(page_text)
            
            for chunk_idx, text_chunk in enumerate(extracted_chunks):
                chunk_id = len(chunks_metadata) + len(new_metadata)
                new_chunks.append(text_chunk)
                new_metadata.append({
                    "chunk_id": chunk_id,
                    "document": file.filename,
                    "page": page_idx + 1,
                    "text": text_chunk
                })
        
        if not new_chunks:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No readable text could be extracted from the uploaded PDF."
            )
        
        # Generate embeddings
        embeddings = embedding_model.encode(new_chunks, convert_to_numpy=True)
        embeddings_np = np.array(embeddings, dtype=np.float32)
        
        # Normalize L2 vectors for Cosine Similarity using Inner Product Index
        faiss.normalize_L2(embeddings_np)
        
        # Add to FAISS index and metadata store
        faiss_index.add(embeddings_np)
        chunks_metadata.extend(new_metadata)
        
        # Save updated vector store
        save_vector_store()
        
        return {
            "filename": file.filename,
            "status": "success",
            "pages_processed": num_pages,
            "chunks_created": len(new_chunks),
            "total_chunks_in_db": faiss_index.ntotal
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error processing PDF upload: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing PDF file: {str(e)}"
        )

@app.post("/search/")
def search_chunks(request: SearchRequest):
    if not request.query or not request.query.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Query parameter cannot be empty."
        )
        
    if faiss_index is None or faiss_index.ntotal == 0:
        return {
            "query": request.query,
            "results_count": 0,
            "results": [],
            "message": "No documents uploaded yet. Please upload a PDF first."
        }
    
    top_k = min(max(1, request.top_k), faiss_index.ntotal)
    
    # Query embedding
    query_vec = embedding_model.encode([request.query], convert_to_numpy=True)
    query_vec_np = np.array(query_vec, dtype=np.float32)
    faiss.normalize_L2(query_vec_np)
    
    # Perform vector search
    scores, indices = faiss_index.search(query_vec_np, top_k)
    
    results = []
    for score, idx in zip(scores[0], indices[0]):
        if idx >= 0 and idx < len(chunks_metadata):
            meta = chunks_metadata[idx]
            # Convert float32 score to regular float
            sim_score = max(0.0, float(score))
            results.append({
                "chunk_id": meta["chunk_id"],
                "document": meta["document"],
                "page": meta["page"],
                "score": round(sim_score, 4),
                "text": meta["text"]
            })
            
    return {
        "query": request.query,
        "results_count": len(results),
        "results": results
    }
