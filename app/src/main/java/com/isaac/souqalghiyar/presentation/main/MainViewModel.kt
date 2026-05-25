package com.isaac.souqalghiyar.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaac.souqalghiyar.domain.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository // حقن الـ Repository فقط! احترافية مطلقة.
) : ViewModel() {

    private val _adsList = MutableStateFlow<List<Advertisement>>(emptyList())
    val adsList: StateFlow<List<Advertisement>> = _adsList.asStateFlow()

    init {
        fetchAdvertisements()
    }

    private fun fetchAdvertisements() {
        viewModelScope.launch {
            repository.getAdvertisements()
                .catch { e -> 
                    // معالجة الأخطاء هنا
                }
                .collect { ads ->
                    _adsList.value = ads
                }
        }
    }
}
