package cl.uct.ojociudadano.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.IncidentRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

import java.util.Collections.emptyList

class HomeViewModel(
    private val repository: IncidentRepository
) : ViewModel() {
    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents = _incidents.asStateFlow()

    fun loadIncidents() {
        viewModelScope.launch {
            try {
                val result = repository.getIncidents()
                _incidents.value = result
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                runCatching {
                    Log.e(
                        "HomeViewModel",
                        "Error al cargar incidentes: type=${exception::class.java.name}, " +
                            "message=${exception.message}, cause=${exception.cause}",
                        exception
                    )
                }
            }
        }
    }
}