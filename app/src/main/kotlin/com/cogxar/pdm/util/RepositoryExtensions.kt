package com.cogxar.pdm.util

import com.cogxar.pdm.data.model.HistoricalReadingsResponse
import com.cogxar.pdm.data.model.Machine
import com.cogxar.pdm.data.repository.CancellableRequest
import com.cogxar.pdm.data.repository.PdmRepository
import com.cogxar.pdm.data.repository.RepositoryCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Coroutine bridge wrapping the RepositoryCallback-based getReadings in suspendCancellableCoroutine.
 * Cancels the underlying in-flight request when the coroutine is cancelled.
 * Cancellation is handled cleanly and never surfaced as an error.
 */
suspend fun PdmRepository.getReadingsSuspend(
    machineId: String,
    timeRange: String,
    cursor: String? = null
): HistoricalReadingsResponse = suspendCancellableCoroutine { continuation ->
    val request: CancellableRequest = getReadings(
        machineId,
        timeRange,
        cursor,
        object : RepositoryCallback<HistoricalReadingsResponse> {
            override fun onSuccess(result: HistoricalReadingsResponse) {
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            override fun onError(throwable: Throwable) {
                if (continuation.isActive) {
                    continuation.resumeWithException(throwable)
                }
            }
        }
    )

    continuation.invokeOnCancellation {
        request.cancel()
    }
}

/**
 * Coroutine bridge for fetching the machines list.
 */
suspend fun PdmRepository.getMachinesSuspend(): List<Machine> = suspendCancellableCoroutine { continuation ->
    val request: CancellableRequest = getMachines(object : RepositoryCallback<List<Machine>> {
        override fun onSuccess(result: List<Machine>) {
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        override fun onError(throwable: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(throwable)
            }
        }
    })

    continuation.invokeOnCancellation {
        request.cancel()
    }
}
