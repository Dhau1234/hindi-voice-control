package com.example.hindivoicecontrol

interface AsrEngine {
    fun startListening(onResult: (String) -> Unit)
    fun stopListening()
}
