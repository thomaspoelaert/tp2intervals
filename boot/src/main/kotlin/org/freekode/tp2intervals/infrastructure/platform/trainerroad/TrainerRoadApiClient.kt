package org.freekode.tp2intervals.infrastructure.platform.trainerroad

import org.freekode.tp2intervals.infrastructure.platform.trainerroad.activity.TrainerRoadActivityDTO
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout.TRFindWorkoutsResponseDTO
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout.TRWorkoutResponseDTO
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout.TRWorkoutProfileZoneDTO
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.core.io.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    value = "TrainerRoadApiClient",
    url = "\${app.trainer-road.api-url}",
    dismiss404 = true,
    primary = false,
    configuration = [TrainerRoadApiClientConfig::class]
)
interface TrainerRoadApiClient {
    @GetMapping(
        value = ["/app/api/react-calendar/{memberId}/timeline"],
        headers = ["trainerroad-jsonformat=camel-case", "tr-cache-control=use-cache"]
    )
    fun getTimeline(
        @PathVariable("memberId") memberId: Long,
        @RequestParam("start") startDate: String,
        @RequestParam("end") endDate: String,
    ): TrainerRoadTimelineDTO

    @GetMapping(
        value = ["/app/api/react-calendar/{memberId}/activities"],
        headers = ["trainerroad-jsonformat=camel-case", "tr-cache-control=use-cache"]
    )
    fun getActivities(
        @PathVariable("memberId") memberId: Long,
        @RequestHeader("ids") ids: String,
    ): List<TrainerRoadActivityDTO>

    @PostMapping(
        value = ["/app/api/workouts"],
        headers = ["trainerroad-jsonformat=camel-case", "tr-cache-control=no-cache"]
    )
    fun findWorkouts(
        @RequestBody requestDTO: TRFindWorkoutsRequestDTO,
    ): TRFindWorkoutsResponseDTO

    @GetMapping(
        value = ["/app/api/workouts/workout-profiles-by-zone"],
        headers = ["trainerroad-jsonformat=camel-case", "tr-cache-control=use-cache"]
    )
    fun getWorkoutProfilesByZone(): List<TRWorkoutProfileZoneDTO>

    @GetMapping("/app/api/workoutdetails/{workoutId}")
    fun getWorkout(
        @PathVariable workoutId: String,
    ): TRWorkoutResponseDTO

    @PostMapping("/app/api/activities/{activityId}/exports/fit")
    fun exportFit(@PathVariable activityId: String): Resource
}
