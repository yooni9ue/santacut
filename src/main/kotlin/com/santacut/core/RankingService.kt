package com.santacut.core

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class RankingService(
    private val assessments: AssessmentRepository,
    private val participants: ParticipantRepository,
    private val regions: RegionRepository,
    private val snapshots: ResultSnapshotRepository,
    private val shares: ShareRepository,
    @Value("\${app.season-year}") private val year: Int,
    @Value("\${app.origin-lat}") private val lat: Double,
    @Value("\${app.origin-lon}") private val lon: Double
) {
    @Transactional
    @Synchronized
    fun rebuild(computedAt: Instant = Instant.now()) {
        val start = ZonedDateTime.of(year,12,25,0,0,0,0,ZoneId.of("Asia/Seoul"))
        if (!computedAt.isBefore(start.toInstant())) return

        val participantById = participants.findAll().associateBy { it.id }
        val regionByCode = regions.findAll().associateBy { it.code }
        val latestAssessments = assessments.findLatestForEachUser()
            .mapNotNull { latest ->
                val participant = participantById[latest.userId] ?: return@mapNotNull null
                val region = regionByCode[participant.regionCode] ?: return@mapNotNull null
                Triple(latest, participant, region)
            }
            .sortedWith(compareByDescending<Triple<AssessmentEntity, ParticipantEntity, RegionEntity>> { it.first.scoreHundredths }
                .thenBy { it.first.createdAt }
                .thenBy { it.first.userId })

        var previous = Coordinate(lat,lon)
        var work = 0.0
        var lastScore = -1
        var rank = 0
        val computed = utcDatabaseTime(computedAt)
        val newSnapshots = latestAssessments.mapIndexed { index, (assessment, participant, region) ->
            val next = Coordinate(region.latitude,region.longitude)
            work += Delivery.distance(previous,next) / 100 * 3600 + 180
            if (assessment.scoreHundredths != lastScore) rank = index + 1
            previous = next
            lastScore = assessment.scoreHundredths
            ResultSnapshotEntity(
                id=UUID.randomUUID().toString(),
                userId=participant.id,
                assessmentId=assessment.id,
                rank=rank,
                deliverySequence=index+1,
                eta=utcDatabaseTime(Delivery.arrival(start,work).toInstant()),
                computedAt=computed
            )
        }
        // Keep the current ranking generation and snapshots captured by shared result URLs.
        // Without pruning, one full ranking copy per analysis makes storage grow quadratically.
        shares.deleteAllByRevokedAtIsNotNull()
        val sharedSnapshotIds = shares.findAllByRevokedAtIsNull().mapTo(HashSet()) { it.snapshotId }
        snapshots.findAll()
            .filterNot { it.id in sharedSnapshotIds }
            .takeIf { it.isNotEmpty() }
            ?.let(snapshots::deleteAllInBatch)
        snapshots.saveAll(newSnapshots)
    }
}
