package com.example.harish332m.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harish332m.data.api.RetrofitClient
import com.example.harish332m.data.model.HealthResponse
import com.example.harish332m.data.model.SearchRequest
import com.example.harish332m.data.model.SearchResponse
import com.example.harish332m.data.model.UploadResponse
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

sealed class UiState {
    object Home : UiState()
    object Processing : UiState()
    data class UploadSuccess(val response: UploadResponse) : UiState()
    object QuestionReady : UiState()
    object Searching : UiState()
    data class SearchResults(val response: SearchResponse) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel : ViewModel() {

    private val apiService = RetrofitClient.instance

    private val _uiState = MutableLiveData<UiState>(UiState.Home)
    val uiState: LiveData<UiState> = _uiState

    private val _healthState = MutableLiveData<HealthResponse?>()
    val healthState: LiveData<HealthResponse?> = _healthState

    init {
        checkHealth()
    }

    fun checkHealth() {
        viewModelScope.launch {
            try {
                val response = apiService.checkHealth()
                if (response.isSuccessful && response.body() != null) {
                    _healthState.value = response.body()
                } else {
                    _healthState.value = null
                }
            } catch (e: Exception) {
                _healthState.value = null
            }
        }
    }

    fun uploadPdf(filePart: MultipartBody.Part) {
        _uiState.value = UiState.Processing
        viewModelScope.launch {
            try {
                val response = apiService.uploadDocument(filePart)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = UiState.UploadSuccess(response.body()!!)
                    checkHealth()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Upload failed with HTTP ${response.code()}"
                    _uiState.value = UiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to connect to backend server.")
            }
        }
    }

    fun askQuestion(query: String) {
        if (query.isBlank()) {
            _uiState.value = UiState.Error("Question cannot be empty.")
            return
        }

        _uiState.value = UiState.Searching
        viewModelScope.launch {
            try {
                val response = apiService.search(SearchRequest(query = query, topK = 3))
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = UiState.SearchResults(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Search failed with HTTP ${response.code()}"
                    _uiState.value = UiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to execute vector search.")
            }
        }
    }

    fun resetToHome() {
        _uiState.value = UiState.Home
        checkHealth()
    }

    fun goToQuestionScreen() {
        _uiState.value = UiState.QuestionReady
    }
}
