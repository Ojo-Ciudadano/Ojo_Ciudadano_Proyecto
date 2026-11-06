package cl.uct.ojociudadano.domain.repository

import cl.uct.ojociudadano.domain.model.LocationResult

interface LocationClient {
    suspend fun getCurrentLocation(): LocationResult
}