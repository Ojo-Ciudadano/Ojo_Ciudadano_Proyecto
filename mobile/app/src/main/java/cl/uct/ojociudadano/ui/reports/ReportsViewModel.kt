package cl.uct.ojociudadano.ui.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ReportsViewModel(
    private val incidentRepository: IncidentRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents = _incidents.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    init {
        loadIncidents()
        loadCategories()
    }

    fun loadIncidents() {
        viewModelScope.launch {
            try {
                val resultI = incidentRepository.getIncidents()
                _incidents.value = resultI
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

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val resultC = categoryRepository.getCategories()
                _categories.value = resultC
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    "ReportsViewModel",
                    "Error al cargar categorías",
                    exception
                )
            }
        }
    }
}