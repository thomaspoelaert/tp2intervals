package org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout

import org.freekode.tp2intervals.app.trainerroadexport.ExportWorkoutSummary
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrainerRoadZwoConverterTest {
    private val converter = TrainerRoadZwoConverter()

    @Test
    fun `preserves relative FTP ranges and produces valid workout XML`() {
        val details = TrainerRoadWorkoutDetailsDTO("1", "Ramp: Workout?", "<p>desc</p>", false, 50, 10, progressionLevel = 4.2)
        val response = TRWorkoutResponseDTO(TRWorkoutResponseDTO.TRWorkout(details, listOf(
            TRWorkoutResponseDTO.IntervalsDataDTO(0.0, 60.0, "Warm up", false, false, 50.0),
            TRWorkoutResponseDTO.IntervalsDataDTO(60.0, 120.0, "Work", false, false, 90.0, listOf(90.0, 100.0)),
        )))
        val summary = ExportWorkoutSummary("1", details.workoutName, "1", "Sweet Spot", "SS", durationMinutes = 10, workoutLevel = 4.2)

        val result = converter.convert(response, summary)

        assertTrue(result.fileName.contains("[SS][WL4.2][10m]"), result.fileName)
        assertTrue(result.xml.contains("PowerLow=\"0.9\""), result.xml)
        assertTrue(result.xml.contains("PowerHigh=\"1\""), result.xml)
        assertTrue(result.xml.contains("Ramp"), result.xml)
        assertTrue(result.xml.contains("Ramp_ Workout_"), result.xml)
    }

    @Test
    fun `maps unknown categories deterministically and uses WLNA`() {
        val details = TrainerRoadWorkoutDetailsDTO("22", "Other", "", false, 1, 1)
        val response = TRWorkoutResponseDTO(TRWorkoutResponseDTO.TRWorkout(details, listOf(
            TRWorkoutResponseDTO.IntervalsDataDTO(0.0, 60.0, "Step", false, false, 75.0),
        )))
        val summary = ExportWorkoutSummary("22", "Other", "99", "New Future Zone", converter.categoryCode("New Future Zone"), durationMinutes = 1)

        val result = converter.convert(response, summary)

        assertTrue(result.fileName.contains("[NEWFUTZO][WLNA][1m]"), result.fileName)
    }
}
