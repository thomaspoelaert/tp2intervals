package org.freekode.tp2intervals.infrastructure.platform.trainerroad

class TRFindWorkoutsRequestDTO(
    val searchText: String,
    val pageNumber: Int,
    val pageSize: Int,
    val isDescending: Boolean = false,
    val sortProperty: String = "workoutName",
    val progressions: Map<String, Any?> = emptyMap(),
    val durations: Map<String, Boolean> = emptyMap(),
    val workoutInstructions: Map<String, Boolean> = emptyMap(),
    val workoutTypes: Map<String, Boolean> = emptyMap(),
)
