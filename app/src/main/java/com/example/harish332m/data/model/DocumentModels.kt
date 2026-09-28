package com.example.harish332m.data.model

import com.google.gson.annotations.SerializedName

data class UploadResponse(
    @SerializedName("filename") val filename: String,
    @SerializedName("status") val status: String,
    @SerializedName("pages_processed") val pagesProcessed: Int,
    @SerializedName("chunks_created") val chunksCreated: Int,
    @SerializedName("total_chunks_in_db") val totalChunksInDb: Int
)

data class SearchRequest(
    @SerializedName("query") val query: String,
    @SerializedName("top_k") val topK: Int = 3
)

data class SearchResultItem(
    @SerializedName("chunk_id") val chunkId: Int,
    @SerializedName("document") val document: String,
    @SerializedName("page") val page: Int,
    @SerializedName("score") val score: Float,
    @SerializedName("text") val text: String
)

data class SearchResponse(
    @SerializedName("query") val query: String,
    @SerializedName("results_count") val resultsCount: Int,
    @SerializedName("results") val results: List<SearchResultItem>,
    @SerializedName("message") val message: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("total_chunks") val totalChunks: Int,
    @SerializedName("indexed_documents") val indexedDocuments: Int
)
