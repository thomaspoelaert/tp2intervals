package org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

class TRWorkoutProfileZoneDTO(
    @JsonProperty("id")
    @JsonAlias("Id")
    val id: Int? = null,
    @JsonProperty("name")
    @JsonAlias("Name", "text", "Text")
    val name: String? = null,
    @JsonProperty("workoutProfileOptions")
    @JsonAlias("WorkoutProfileOptions", "profiles")
    val workoutProfileOptions: List<TRWorkoutProfileDTO> = emptyList(),
)

class TRWorkoutProfileDTO(
    @JsonProperty("id")
    @JsonAlias("Id")
    val id: Int? = null,
    @JsonProperty("name")
    @JsonAlias("Name", "text", "Text")
    val name: String? = null,
    val durations: List<Any> = emptyList(),
)
