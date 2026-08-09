package org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout

import config.mock.ObjectMapperFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TRFindWorkoutsResponseDTOTest {
    private val mapper = ObjectMapperFactory.objectMapper()

    @Test
    fun `decodes current TrainerRoad camel case response`() {
        val response = mapper.readValue(
            """{"workouts":[{"id":"42","workoutName":"Test","workoutDescription":null,"tags":null,"duration":60,"progressionId":3}],"predicate":{"totalCount":123},"workoutsCacheUnavailable":false}""",
            TRFindWorkoutsResponseDTO::class.java,
        )

        assertEquals(1, response.workouts.size)
        assertEquals("42", response.workouts.single().id)
        assertNull(response.workouts.single().workoutDescription)
        assertNull(response.workouts.single().tags)
        assertEquals(123, response.predicate?.totalCount)
    }

    @Test
    fun `keeps compatibility with legacy Pascal case response`() {
        val response = mapper.readValue(
            """{"Workouts":[{"Id":"7","WorkoutName":"Legacy"}],"Predicate":{"TotalCount":1}}""",
            TRFindWorkoutsResponseDTO::class.java,
        )

        assertEquals("7", response.workouts.single().id)
        assertEquals(1, response.predicate?.totalCount)
    }
}
