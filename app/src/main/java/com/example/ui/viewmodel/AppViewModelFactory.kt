package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.ConserverieRepository

class AppViewModelFactory(
    private val repository: ConserverieRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(VentesViewModel::class.java) ->
                VentesViewModel(repository) as T
            modelClass.isAssignableFrom(ClientsViewModel::class.java) ->
                ClientsViewModel(repository) as T
            modelClass.isAssignableFrom(StockViewModel::class.java) ->
                StockViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
