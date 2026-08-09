package org.freekode.tp2intervals.rest.trainerroadexport

import org.freekode.tp2intervals.app.trainerroadexport.ExportScanRequest
import org.freekode.tp2intervals.app.trainerroadexport.ExportStartRequest
import org.freekode.tp2intervals.app.trainerroadexport.TrainerRoadZwoExportService
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TrainerRoadZwoExportController(private val service: TrainerRoadZwoExportService) {
    @PostMapping("/api/trainer-road/zwo-export/scans")
    fun startScan(@RequestBody(required = false) request: ExportScanRequest?) = service.startScan(request ?: ExportScanRequest())

    @GetMapping("/api/trainer-road/zwo-export/scans/{id}")
    fun scan(@PathVariable id: String) = service.scan(id)

    @PostMapping("/api/trainer-road/zwo-export/jobs")
    fun startJob(@RequestBody request: ExportStartRequest) = service.startJob(request)

    @GetMapping("/api/trainer-road/zwo-export/jobs/{id}")
    fun job(@PathVariable id: String) = service.job(id)

    @PostMapping("/api/trainer-road/zwo-export/jobs/{id}/cancel")
    fun cancel(@PathVariable id: String) = service.cancel(id)

    @GetMapping("/api/trainer-road/zwo-export/jobs/{id}/download")
    fun download(@PathVariable id: String): ResponseEntity<Resource> {
        val resource = service.download(id)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("trainerroad-zwo-export.zip").build().toString())
            .body(resource)
    }
}
