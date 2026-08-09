package org.freekode.tp2intervals.app.trainerroadexport

import com.fasterxml.jackson.databind.ObjectMapper
import feign.FeignException
import feign.codec.DecodeException
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.TrainerRoadApiClient
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.TRFindWorkoutsRequestDTO
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout.TRWorkoutProfileZoneDTO
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout.TRWorkoutResponseDTO
import org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout.TrainerRoadZwoConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class TrainerRoadZwoExportService(
    private val api: TrainerRoadApiClient,
    private val mapper: ObjectMapper,
    private val converter: TrainerRoadZwoConverter,
    @Value("\${app.trainer-road.export-root:\${user.home}/.tp2intervals/trainerroad-zwo-export}") root: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "trainerroad-zwo-export").apply { isDaemon = true } }
    private val rootPath = Path.of(root)
    private val scans = ConcurrentHashMap<String, ScanState>()
    private val jobs = ConcurrentHashMap<String, JobState>()

    fun startScan(request: ExportScanRequest): ExportScanSummary {
        val id = UUID.randomUUID().toString()
        val state = ScanState(id, clampDelay(request.requestDelaySeconds))
        scans[id] = state
        executor.submit { runScan(state) }
        return state.summary()
    }

    fun scan(id: String): ExportScanSummary = scans[id]?.summary() ?: throw NoSuchElementException("Unknown scan")

    fun startJob(request: ExportStartRequest): ExportJobSummary {
        val scan = scans[request.scanId] ?: throw NoSuchElementException("Unknown scan")
        check(scan.status == "completed") { "Scan is not complete" }
        check(jobs.values.none { it.status == "running" || it.status == "queued" }) { "Another export is active" }
        val selected = scan.workouts.filter { workout ->
            request.selectedWorkoutIds.contains(workout.id) &&
                (request.categories.isEmpty() || request.categories.contains(workout.categoryId)) &&
                (request.minDuration == null || workout.durationMinutes >= request.minDuration) &&
                (request.maxDuration == null || workout.durationMinutes <= request.maxDuration) &&
                (request.minWorkoutLevel == null || (workout.workoutLevel ?: Double.NaN) >= request.minWorkoutLevel) &&
                (request.maxWorkoutLevel == null || (workout.workoutLevel ?: Double.NaN) <= request.maxWorkoutLevel)
        }
        val state = JobState(UUID.randomUUID().toString(), request.scanId, selected, clampDelay(request.requestDelaySeconds), request.forceRefresh)
        jobs[state.id] = state
        executor.submit { runJob(state) }
        return state.summary()
    }

    fun job(id: String): ExportJobSummary = jobs[id]?.summary() ?: throw NoSuchElementException("Unknown job")

    fun cancel(id: String): ExportJobSummary {
        val state = jobs[id] ?: throw NoSuchElementException("Unknown job")
        state.cancelled = true
        return state.summary()
    }

    fun download(id: String): Resource {
        val state = jobs[id] ?: throw NoSuchElementException("Unknown job")
        check(state.zipPath != null && Files.exists(state.zipPath)) { "ZIP is not ready" }
        return FileSystemResource(state.zipPath!!)
    }

    private fun runScan(state: ScanState) {
        state.status = "running"
        try {
            val catalog = api.getWorkoutProfilesByZone()
            state.catalog = catalog.filter { it.id != null && !it.name.isNullOrBlank() }
            val all = mutableListOf<ExportWorkoutSummary>()
            var page = 0
            var total: Int? = null
            do {
                if (state.cancelled) return state.finish("cancelled")
                state.requestCount++
                val response = api.findWorkouts(TRFindWorkoutsRequestDTO("", page, 100, false, "workoutName", libraryPredicate()))
                state.completedRequests++
                val rows = response.workouts.mapNotNull { toSummary(it, state.catalog) }
                all += rows
                total = response.predicate?.totalCount ?: total
                page++
                state.totalWorkouts = total ?: all.size
                state.workouts = all.toList()
                val knownTotal = total
                if (rows.isEmpty() || (knownTotal != null && all.size >= knownTotal)) break
                pause(state.delaySeconds)
            } while (true)
            state.workouts = all.distinctBy { it.id }
            state.finish("completed")
        } catch (e: Throwable) {
            state.errorCode = errorCode(e)
            log.error("TrainerRoad library scan {} failed with {}", state.id, state.errorCode, e)
            state.finish("failed")
        }
    }

    private fun runJob(state: JobState) {
        state.status = "running"
        try {
            Files.createDirectories(rootPath.resolve("cache"))
            val outputRoot = rootPath.resolve("runs").resolve(state.id)
            Files.createDirectories(outputRoot)
            val generated = mutableListOf<Path>()
            state.workouts.forEachIndexed { index, summary ->
                if (state.cancelled) return state.finish("cancelled")
                state.currentWorkout = summary.name
                try {
                    val detailPath = rootPath.resolve("cache").resolve("${safeId(summary.id)}.json")
                    val detail = if (!state.forceRefresh && Files.exists(detailPath)) {
                        mapper.readValue(detailPath.toFile(), TRWorkoutResponseDTO::class.java)
                    } else {
                        val fetched = api.getWorkout(summary.id)
                        Files.writeString(detailPath, mapper.writeValueAsString(fetched))
                        if (index < state.workouts.size - 1) pause(state.delaySeconds)
                        fetched
                    }
                    val converted = converter.convert(detail, summary)
                    val categoryDir = outputRoot.resolve(converted.categoryFolder)
                    Files.createDirectories(categoryDir)
                    val output = categoryDir.resolve(uniqueName(categoryDir, converted.fileName, summary.id))
                    Files.writeString(output, converted.xml)
                    generated.add(output)
                    state.exported++
                    state.manifest.add(ManifestEntry(summary.id, summary.name, summary.category, summary.workoutLevel, summary.durationMinutes, "exported", output.toString(), Instant.now().toString(), converted.warnings))
                } catch (e: IllegalArgumentException) {
                    state.unsupported++
                    state.manifest.add(ManifestEntry(summary.id, summary.name, summary.category, summary.workoutLevel, summary.durationMinutes, "unsupported", null, Instant.now().toString(), listOf(e.message ?: "unsupported")))
                } catch (e: Throwable) {
                    state.failed++
                    state.manifest.add(ManifestEntry(summary.id, summary.name, summary.category, summary.workoutLevel, summary.durationMinutes, "failed", null, Instant.now().toString(), listOf(errorCode(e))))
                    if (e is FeignException && (e.status() == 401 || e.status() == 403)) throw e
                } finally {
                    state.processed++
                }
            }
            writeManifest(state)
            state.zipPath = outputRoot.resolveSibling("${state.id}.zip")
            zip(generated, outputRoot, state.zipPath!!)
            state.finish("completed")
        } catch (e: Throwable) {
            state.errorCode = errorCode(e)
            log.error("TrainerRoad ZWO export {} failed with {}", state.id, state.errorCode, e)
            state.finish(if (state.cancelled) "cancelled" else "failed")
        }
    }

    private fun toSummary(row: org.freekode.tp2intervals.infrastructure.platform.trainerroad.workout.TrainerRoadWorkoutDetailsDTO, catalog: List<TRWorkoutProfileZoneDTO>): ExportWorkoutSummary? {
        if (row.id.isBlank()) return null
        val categoryId = row.progressionId?.toString() ?: "uncategorized"
        val category = catalog.firstOrNull { it.id?.toString() == categoryId }?.name ?: "Uncategorized"
        return ExportWorkoutSummary(row.id, row.workoutName, categoryId, category, converter.categoryCode(category), row.profileId, row.profileName, row.duration, row.progressionLevel, row.intensityFactor?.let { if (it > 2) it / 100 else it }, row.tss, row.tags.orEmpty().map { it.toString() })
    }

    private fun libraryPredicate() = mapOf("profileIds" to emptyList<Int>(), "progressionIds" to emptyList<Int>(), "progressionLevels" to emptyList<Double>())

    private fun writeManifest(state: JobState) {
        Files.createDirectories(rootPath)
        val manifest = rootPath.resolve("export-manifest.json")
        val temp = manifest.resolveSibling("export-manifest.json.tmp")
        Files.writeString(temp, mapper.writeValueAsString(ManifestFile(state.id, Instant.now().toString(), state.manifest)))
        Files.move(temp, manifest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun zip(files: List<Path>, base: Path, destination: Path) {
        java.util.zip.ZipOutputStream(Files.newOutputStream(destination)).use { output ->
            files.forEach { file ->
                val entry = java.util.zip.ZipEntry(base.relativize(file).toString().replace('\\', '/'))
                output.putNextEntry(entry)
                Files.copy(file, output)
                output.closeEntry()
            }
        }
    }

    private fun uniqueName(dir: Path, name: String, id: String): String {
        if (!Files.exists(dir.resolve(name))) return name
        return name.removeSuffix(".zwo") + "-TR${safeId(id)}.zwo"
    }

    private fun safeId(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun pause(seconds: Long) { if (seconds > 0) TimeUnit.SECONDS.sleep(seconds.coerceAtMost(300)) }
    private fun clampDelay(value: Long) = value.coerceIn(0, 300)
    private fun errorCode(error: Throwable): String = when (error) {
        is DecodeException -> "trainerroad_response_invalid"
        is FeignException -> when (error.status()) { 401, 403 -> "auth_expired"; 429 -> "rate_limited"; else -> "trainerroad_http_${error.status()}" }
        else -> if (error.cause is com.fasterxml.jackson.core.JacksonException) "trainerroad_response_invalid" else "network_or_internal_error"
    }

    private data class ManifestFile(val runId: String, val updatedAt: String, val workouts: List<ManifestEntry>)
    private data class ManifestEntry(val trainerRoadId: String, val name: String, val category: String, val workoutLevel: Double?, val durationMinutes: Int, val status: String, val outputFile: String?, val timestamp: String, val warnings: List<String>)

    private inner class ScanState(val id: String, val delaySeconds: Long) {
        @Volatile var status = "queued"; @Volatile var requestCount = 0; @Volatile var completedRequests = 0; @Volatile var totalWorkouts = 0; @Volatile var workouts: List<ExportWorkoutSummary> = emptyList(); @Volatile var catalog: List<TRWorkoutProfileZoneDTO> = emptyList(); @Volatile var errorCode: String? = null; @Volatile var cancelled = false
        fun finish(value: String) { status = value }
        fun summary() = ExportScanSummary(id, status, requestCount, completedRequests, totalWorkouts, catalog.map { zone -> ExportCategory(zone.id.toString(), zone.name ?: "Uncategorized", workouts.count { it.categoryId == zone.id.toString() }, zone.name?.let { converter.categoryCode(it) } ?: "OTHER") }, workouts, errorCode)
    }

    private class JobState(val id: String, val scanId: String, val workouts: List<ExportWorkoutSummary>, val delaySeconds: Long, val forceRefresh: Boolean) {
        @Volatile var status = "queued"; @Volatile var processed = 0; @Volatile var exported = 0; @Volatile var skipped = 0; @Volatile var failed = 0; @Volatile var unsupported = 0; @Volatile var currentWorkout: String? = null; @Volatile var errorCode: String? = null; @Volatile var cancelled = false; @Volatile var zipPath: Path? = null
        val manifest = mutableListOf<ManifestEntry>()
        fun finish(value: String) { status = value }
        fun summary() = ExportJobSummary(id, scanId, status, workouts.size, processed, exported, skipped, failed, unsupported, currentWorkout, errorCode, zipPath != null && Files.exists(zipPath))
    }
}
