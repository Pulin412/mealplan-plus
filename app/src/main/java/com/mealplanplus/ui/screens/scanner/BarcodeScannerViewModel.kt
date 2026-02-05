package com.mealplanplus.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.OpenFoodFactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = true,
    val isLoading: Boolean = false,
    val scannedBarcode: String? = null,
    val foundFood: FoodItem? = null,
    val existingFood: FoodItem? = null,
    val error: String? = null,
    val foodSaved: Boolean = false
)

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val openFoodFactsRepository: OpenFoodFactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onBarcodeScanned(barcode: String) {
        if (_uiState.value.isLoading || _uiState.value.scannedBarcode == barcode) return

        _uiState.update { it.copy(isScanning = false, isLoading = true, scannedBarcode = barcode) }

        viewModelScope.launch {
            // Check local database first
            val existingFood = foodRepository.getFoodByBarcode(barcode)
            if (existingFood != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        existingFood = existingFood,
                        foundFood = null,
                        error = null
                    )
                }
                return@launch
            }

            // Fetch from OpenFoodFacts API
            val result = openFoodFactsRepository.getProductByBarcode(barcode)
            result.fold(
                onSuccess = { food ->
                    if (food != null) {
                        _uiState.update {
                            it.copy(isLoading = false, foundFood = food, error = null)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = "Product not found in database")
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to fetch product")
                    }
                }
            )
        }
    }

    fun saveFood(food: FoodItem) {
        viewModelScope.launch {
            foodRepository.insertFood(food)
            _uiState.update { it.copy(foodSaved = true) }
        }
    }

    fun resetScanner() {
        _uiState.value = ScannerUiState()
    }

    fun retryScanning() {
        _uiState.update {
            it.copy(
                isScanning = true,
                scannedBarcode = null,
                foundFood = null,
                existingFood = null,
                error = null
            )
        }
    }
}
