package com.example.harish332m

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.harish332m.databinding.ActivityMainBinding
import com.example.harish332m.ui.adapter.SearchResultAdapter
import com.example.harish332m.ui.viewmodel.MainViewModel
import com.example.harish332m.ui.viewmodel.UiState
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: SearchResultAdapter

    private val selectPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processAndUploadPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter()
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnUploadPdf.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        binding.btnProceedToAsk.setOnClickListener {
            viewModel.goToQuestionScreen()
        }

        binding.btnSearch.setOnClickListener {
            val query = binding.etQuestion.text?.toString()?.trim() ?: ""
            if (query.isNotEmpty()) {
                viewModel.askQuestion(query)
            } else {
                Toast.makeText(this, "Please enter a question.", Toast.LENGTH_SHORT).show()
            }
        }

        // Sample Question Chips
        binding.chipQ1.setOnClickListener {
            binding.etQuestion.setText(binding.chipQ1.text)
        }
        binding.chipQ2.setOnClickListener {
            binding.etQuestion.setText(binding.chipQ2.text)
        }
        binding.chipQ3.setOnClickListener {
            binding.etQuestion.setText(binding.chipQ3.text)
        }

        binding.btnAskAnother.setOnClickListener {
            binding.etQuestion.text?.clear()
            viewModel.goToQuestionScreen()
        }

        binding.btnUploadNew.setOnClickListener {
            viewModel.resetToHome()
            selectPdfLauncher.launch("application/pdf")
        }

        binding.btnRetry.setOnClickListener {
            viewModel.resetToHome()
        }
    }

    private fun observeViewModel() {
        viewModel.healthState.observe(this) { health ->
            if (health != null && health.status == "ok") {
                binding.tvHealthStatus.text = "Online • ${health.totalChunks} Chunks"
                binding.tvHealthStatus.setBackgroundResource(R.color.success_green_bg)
                binding.tvHealthStatus.setTextColor(getColor(R.color.success_green))
            } else {
                binding.tvHealthStatus.text = "Offline"
                binding.tvHealthStatus.setBackgroundResource(R.color.error_red_bg)
                binding.tvHealthStatus.setTextColor(getColor(R.color.error_red))
            }
        }

        viewModel.uiState.observe(this) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: UiState) {
        binding.cardHome.visibility = View.GONE
        binding.cardProcessing.visibility = View.GONE
        binding.cardUploadSuccess.visibility = View.GONE
        binding.cardAskQuestion.visibility = View.GONE
        binding.cardSearching.visibility = View.GONE
        binding.cardResults.visibility = View.GONE
        binding.cardError.visibility = View.GONE

        when (state) {
            is UiState.Home -> {
                binding.cardHome.visibility = View.VISIBLE
            }
            is UiState.Processing -> {
                binding.cardProcessing.visibility = View.VISIBLE
            }
            is UiState.UploadSuccess -> {
                binding.cardUploadSuccess.visibility = View.VISIBLE
                val res = state.response
                binding.tvSuccessFilename.text = "Filename: ${res.filename}"
                binding.tvStatPages.text = res.pagesProcessed.toString()
                binding.tvStatChunks.text = res.chunksCreated.toString()
                binding.tvStatTotal.text = res.totalChunksInDb.toString()
            }
            is UiState.QuestionReady -> {
                binding.cardAskQuestion.visibility = View.VISIBLE
            }
            is UiState.Searching -> {
                binding.cardSearching.visibility = View.VISIBLE
            }
            is UiState.SearchResults -> {
                binding.cardResults.visibility = View.VISIBLE
                val res = state.response
                binding.tvQueryLabel.text = "Query: \"${res.query}\""

                if (res.results.isEmpty()) {
                    binding.cardEmptyResults.visibility = View.VISIBLE
                    binding.rvResults.visibility = View.GONE
                    binding.tvEmptyMsg.text = res.message ?: "No matching chunks found in FAISS vector store."
                } else {
                    binding.cardEmptyResults.visibility = View.GONE
                    binding.rvResults.visibility = View.VISIBLE
                    adapter.submitList(res.results)
                }
            }
            is UiState.Error -> {
                binding.cardError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    private fun processAndUploadPdf(uri: Uri) {
        try {
            val contentResolver = applicationContext.contentResolver
            val fileName = getFileName(uri)
            val tempFile = File(cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val requestFile = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", fileName, requestFile)

            viewModel.uploadPdf(body)
        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex("_display_name")
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "uploaded_document.pdf"
    }
}