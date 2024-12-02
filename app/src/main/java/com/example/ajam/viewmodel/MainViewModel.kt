package com.example.ajam.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap

    private val _classificationResult = MutableStateFlow("")
    val classificationResult: StateFlow<String> = _classificationResult

    private val _limeExplanationBitmap = MutableStateFlow<Bitmap?>(null)
    val limeExplanationBitmap: StateFlow<Bitmap?> = _limeExplanationBitmap

    private val client = OkHttpClient.Builder()
        .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .build()


    private val baseUrl = "https://6e8b-200-215-249-209.ngrok-free.app" // Cambia por tu URL de Ngrok o IP pública.

    fun setSelectedImage(bitmap: Bitmap) {
        _selectedImageBitmap.value = bitmap
    }

    fun classifyImage() {
        val bitmap = _selectedImageBitmap.value ?: return
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Guardar el bitmap como archivo temporal
                val file = File(context.cacheDir, "temp_image.jpg")
                file.outputStream().use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // Construir la solicitud HTTP
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", file.name,
                        file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/predict")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    _classificationResult.value = responseBody ?: "Sin respuesta"
                } else {
                    _classificationResult.value = "Error en la clasificación"
                }
            } catch (e: Exception) {
                _classificationResult.value = "Error: ${e.message}"
            }
        }
    }

    fun getLimeExplanation() {
        val bitmap = _selectedImageBitmap.value ?: return
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Guardar el bitmap como archivo temporal
                val file = File(context.cacheDir, "temp_image.jpg")
                file.outputStream().use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // Construir la solicitud HTTP
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", file.name,
                        file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/explain")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val inputStream: InputStream = response.body?.byteStream() ?: return@launch
                    val explanationBitmap = BitmapFactory.decodeStream(inputStream)
                    _limeExplanationBitmap.value = explanationBitmap
                } else {
                    _classificationResult.value = "Error al generar explicación"
                }
            } catch (e: Exception) {
                _classificationResult.value = "Error: ${e.message}"
            }
        }
    }
}
