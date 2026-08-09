package org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

class TrainerRoadWorkoutDetailsDTO(
    @JsonProperty("Id")
    @JsonAlias("id")
    val id: String,
    @JsonProperty("WorkoutName")
    @JsonAlias("workoutName")
    val workoutName: String,
    @JsonProperty("WorkoutDescription")
    @JsonAlias("workoutDescription")
    val workoutDescription: String? = null,
    @JsonProperty("IsOutside")
    @JsonAlias("isOutside")
    val isOutside: Boolean = false,
    @JsonProperty("Tss")
    @JsonAlias("tss", "TSS")
    val tss: Int = 0,
    @JsonProperty("Duration")
    @JsonAlias("duration")
    val duration: Int = 0,
    @JsonProperty("IntensityFactor")
    @JsonAlias("intensityFactor")
    val intensityFactor: Double? = null,
    @JsonProperty("ProgressionId")
    @JsonAlias("progressionId")
    val progressionId: Int? = null,
    @JsonProperty("ProgressionLevel")
    @JsonAlias("progressionLevel")
    val progressionLevel: Double? = null,
    @JsonProperty("ProfileId")
    @JsonAlias("profileId")
    val profileId: Int? = null,
    @JsonProperty("ProfileName")
    @JsonAlias("profileName")
    val profileName: String? = null,
    @JsonProperty("WorkoutLabelId")
    @JsonAlias("workoutLabelId")
    val workoutLabelId: Int? = null,
    @JsonProperty("Tags")
    @JsonAlias("tags")
    val tags: List<Any>? = null,
)
