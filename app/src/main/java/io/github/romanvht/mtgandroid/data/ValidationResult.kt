package io.github.romanvht.mtgandroid.data

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)