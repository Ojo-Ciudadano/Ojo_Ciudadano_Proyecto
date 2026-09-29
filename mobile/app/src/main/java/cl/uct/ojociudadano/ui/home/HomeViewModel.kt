package cl.uct.ojociudadano.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.IncidentRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import java.util.Collections.emptyList

class HomeViewModel(
    private val repository: IncidentRepository
) : ViewModel() {
    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents = _incidents.asStateFlow()

    fun loadIncidents() {
        viewModelScope.launch {
            val result = repository.getIncidents()
            _incidents.value = result
        }
    }
}