package org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout

import org.springframework.stereotype.Component
import java.io.StringWriter
import java.util.Locale
import javax.xml.stream.XMLOutputFactory

data class ConvertedZwo(val fileName: String, val categoryFolder: String, val xml: String, val warnings: List<String>)

@Component
class TrainerRoadZwoConverter {
    private val zoneCodes = mapOf(
        "recovery" to "REC", "endurance" to "END", "tempo" to "TEMPO",
        "sweet spot" to "SS", "sweetspot" to "SS", "threshold" to "THR",
        "vo2 max" to "VO2", "vo2max" to "VO2", "anaerobic" to "ANA", "sprint" to "SPR",
    )

    fun categoryCode(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        return zoneCodes[normalized] ?: normalized
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString("") { it.take(3) }
            .filter { it.isLetterOrDigit() }
            .uppercase(Locale.ROOT)
            .take(8)
            .ifBlank { "OTHER" }
    }

    fun convert(workout: TRWorkoutResponseDTO, summary: org.freekode.tp2intervals.app.trainerroadexport.ExportWorkoutSummary): ConvertedZwo {
        val details = workout.workout.details
        val raw = workout.workout.intervalData.filterNot { it.name.equals("Workout", true) }
        require(raw.isNotEmpty()) { "No interval structure" }
        val normalized = normalize(raw)
        val warnings = mutableListOf<String>()
        val writer = StringWriter()
        val xml = XMLOutputFactory.newFactory().createXMLStreamWriter(writer)
        val displayName = "[${summary.zoneCode}][WL${summary.workoutLevel?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "NA"}][${summary.durationMinutes}m] ${sanitize(details.workoutName)}"
        xml.writeStartDocument("UTF-8", "1.0")
        xml.writeStartElement("workout_file")
        element(xml, "author", "TrainerRoad via tp2intervals")
        element(xml, "name", displayName)
        element(xml, "description", details.workoutDescription.orEmpty().replace(Regex("<[^>]*>"), " ").trim())
        element(xml, "sportType", "bike")
        xml.writeEmptyElement("tags")
        xml.writeStartElement("workout")
        for (interval in normalized) {
            val low = interval.targetStart() / 100.0
            val high = interval.targetEnd() / 100.0
            val duration = (interval.end - interval.start).toLong()
            if (interval.testInterval && low <= 0.0 && high <= 0.0) {
                xml.writeEmptyElement("FreeRide")
                xml.writeAttribute("Duration", duration.toString())
                warnings += "Test interval exported as FreeRide"
            } else if (interval.testInterval) {
                warnings += "Test interval semantics approximated as power target"
                writeBlock(xml, interval, duration, low, high)
            } else {
                writeBlock(xml, interval, duration, low, high)
            }
        }
        xml.writeEndElement()
        xml.writeEndElement()
        xml.writeEndDocument()
        xml.close()
        val fileName = "${sanitize(displayName)}.zwo"
        return ConvertedZwo(fileName, sanitize(summary.category), writer.toString(), warnings)
    }

    private fun writeBlock(xml: javax.xml.stream.XMLStreamWriter, interval: TRWorkoutResponseDTO.IntervalsDataDTO, duration: Long, low: Double, high: Double) {
        val element = when {
            interval.startTarget != null && interval.startTarget.size > 1 && interval.startTarget[0] != interval.startTarget[1] -> "Ramp"
            interval.startTargetPowerPercent != interval.targetEnd().toDouble() -> "Ramp"
            interval.name.contains("warm", true) -> "Warmup"
            interval.name.contains("cool", true) -> "Cooldown"
            else -> "SteadyState"
        }
        xml.writeEmptyElement(element)
        xml.writeAttribute("Duration", duration.toString())
        if (element == "SteadyState") xml.writeAttribute("Power", format(low))
        else {
            xml.writeAttribute("PowerLow", format(low))
            xml.writeAttribute("PowerHigh", format(high))
        }
    }

    private fun normalize(intervals: List<TRWorkoutResponseDTO.IntervalsDataDTO>): List<TRWorkoutResponseDTO.IntervalsDataDTO> {
        require(intervals.all { it.end >= it.start }) { "Invalid interval length" }
        val sorted = intervals.sortedWith(compareBy({ it.start }, { -(it.end - it.start) }))
        val wrappers = sorted.filter { outer -> sorted.any { inner -> inner !== outer && inner.start >= outer.start && inner.end <= outer.end && (inner.start > outer.start || inner.end < outer.end) } }
        val leaves = sorted.filterNot { it in wrappers }
        require(leaves.isNotEmpty()) { "No leaf intervals" }
        require(leaves.zipWithNext().none { it.second.start < it.first.end }) { "Overlapping intervals" }
        return leaves
    }

    private fun format(value: Double) = String.format(Locale.ROOT, "%.6f", value).trimEnd('0').trimEnd('.')

    private fun element(xml: javax.xml.stream.XMLStreamWriter, name: String, value: String) {
        xml.writeStartElement(name); xml.writeCharacters(value); xml.writeEndElement()
    }

    private fun sanitize(value: String): String = value
        .replace(Regex("[<>:\"/\\\\|?*\\u0000-\\u001F]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim().trimEnd('.', ' ')
        .ifBlank { "Workout" }
        .take(180)
}
