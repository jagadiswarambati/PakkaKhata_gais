package com.example.presentation.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PakkaKhataDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ObligationEntity
import com.example.data.perception.AndroidSpeechRecognizerProvider
import com.example.data.perception.SpeechState
import com.example.domain.perception.VoiceCreditParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val speechRecognizerProvider = AndroidSpeechRecognizerProvider(application)
    private val voiceCreditParser = VoiceCreditParser()
    private val database = PakkaKhataDatabase.getInstance(application)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _parsedData = MutableStateFlow<VoiceCreditParser.ParsedCredit?>(null)
    val parsedData: StateFlow<VoiceCreditParser.ParsedCredit?> = _parsedData.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun startListening() {
        viewModelScope.launch {
            _isListening.value = true
            _transcript.value = ""
            _parsedData.value = null
            _saveSuccess.value = false

            speechRecognizerProvider.startListening().collect { state ->
                when (state) {
                    is SpeechState.Listening -> {
                        _isListening.value = true
                    }
                    is SpeechState.Success -> {
                        _isListening.value = false
                        _transcript.value = state.transcript
                        _parsedData.value = voiceCreditParser.parse(state.transcript)
                    }
                    is SpeechState.Error -> {
                        _isListening.value = false
                        _transcript.value = state.message
                    }
                }
            }
        }
    }

    fun saveVoiceObligation(onSaved: () -> Unit = {}) {
        val data = _parsedData.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val customerDao = database.customerDao()
            val obligationDao = database.obligationDao()

            // 1. Check if customer exists or create new one
            var customer = customerDao.getCustomerByName(data.customerName)
            val customerId = customer?.id ?: run {
                val newId = UUID.randomUUID().toString()
                val newCustomer = CustomerEntity(
                    id = newId,
                    name = data.customerName,
                    phone = ""
                )
                customerDao.insertCustomer(newCustomer)
                newId
            }

            // 2. Create and insert OPEN obligation
            val obligation = ObligationEntity(
                id = UUID.randomUUID().toString(),
                customerId = customerId,
                amount = data.amount,
                status = "OPEN",
                type = if (data.isCredit) "CREDIT" else "DEBIT",
                createdAt = System.currentTimeMillis()
            )
            obligationDao.insertObligation(obligation)

            _saveSuccess.value = true
            _parsedData.value = null
            _transcript.value = ""

            launch(Dispatchers.Main) {
                onSaved()
            }
        }
    }
}