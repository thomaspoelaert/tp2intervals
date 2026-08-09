package org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

class TRFindWorkoutsResponseDTO(
    @JsonProperty("workouts")
    @JsonAlias("Workouts")
    val workouts: List<TrainerRoadWorkoutDetailsDTO> = emptyList(),
    @JsonProperty("predicate")
    @JsonAlias("Predicate")
    val predicate: PredicateDTO? = null,
)

class PredicateDTO(
    @JsonProperty("totalCount")
    @JsonAlias("TotalCount")
    val totalCount: Int? = null,
)
