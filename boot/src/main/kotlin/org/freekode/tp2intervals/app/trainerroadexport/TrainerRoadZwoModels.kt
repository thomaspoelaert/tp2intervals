package org.freekode.tp2intervals.app.trainerroadexport

data class ExportScanRequest(val requestDelaySeconds: Long = 10)

data class ExportStartRequest(
    val scanId: String,
    val selectedWorkoutIds: List<String> = emptyList(),
    val minDuration: Int? = null,
    val maxDuration: Int? = null,
    val minWorkoutLevel: Double? = null,
    val maxWorkoutLevel: Double? = null,
    val categories: List<String> = emptyList(),
    val requestDelaySeconds: Long = 10,
    val forceRefresh: Boolean = false,
)

data class ExportScanSummary(
    val id: String,
    val status: String,
    val requestCount: Int = 0,
    val completedRequests: Int = 0,
    val totalWorkouts: Int = 0,
    val categories: List<ExportCategory> = emptyList(),
    val workouts: List<ExportWorkoutSummary> = emptyList(),
    val errorCode: String? = null,
)

data class ExportCategory(
    val id: String,
    val name: String,
    val count: Int,
    val zoneCode: String,
)

data class ExportWorkoutSummary(
    val id: String,
    val name: String,
    val categoryId: String,
    val category: String,
    val zoneCode: String,
    val profileId: Int? = null,
    val profileName: String? = null,
    val durationMinutes: Int,
    val workoutLevel: Double? = null,
    val intensityFactor: Double? = null,
    val tss: Int? = null,
    val tags: List<String> = emptyList(),
)

data class ExportJobSummary(
    val id: String,
    val scanId: String,
    val status: String,
    val total: Int,
    val processed: Int,
    val exported: Int,
    val skipped: Int,
    val failed: Int,
    val unsupported: Int,
    val currentWorkout: String? = null,
    val errorCode: String? = null,
    val downloadReady: Boolean = false,
)
