package com.santacut.domain.ranking.service

import com.santacut.domain.analysis.model.Assessments
import com.santacut.domain.global.config.AppProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.Coordinate
import com.santacut.domain.participant.model.Participants
import com.santacut.domain.participant.model.Regions
import com.santacut.domain.ranking.model.RankedResult
import com.santacut.domain.ranking.model.RankingPage
import com.santacut.domain.ranking.model.RankingRows
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.ranking.model.ResultSnapshots
import com.santacut.domain.ranking.model.Season
import com.santacut.domain.share.model.Shares
import com.santacut.infrastructure.analysis.repository.AssessmentRepository
import com.santacut.infrastructure.participant.repository.ParticipantRepository
import com.santacut.infrastructure.participant.repository.RegionRepository
import com.santacut.infrastructure.ranking.repository.ResultSnapshotRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RankingService(
    private val assessmentRepository: AssessmentRepository,
    private val participantRepository: ParticipantRepository,
    private val regionRepository: RegionRepository,
    private val resultSnapshotRepository: ResultSnapshotRepository,
    private val appProperties: AppProperties,
) {
    /**
     * 참가자별 최신 평가로 순위와 배송 예정 시각을 새로 계산해 저장한다.
     * 배송이 시작된 뒤에는 순위를 고정한다. 동시에 두 번 계산하지 않도록 직렬화한다.
     * [shares]는 지우면 안 되는 스냅숏을 알려 주는 현재 공유 링크 목록이다.
     */
    @Synchronized
    fun rebuild(computedAt: Instant, shares: Shares) {
        val season = Season(appProperties.seasonYear)
        if (season.isClosedAt(computedAt)) return
        val entries = Assessments(assessmentRepository.findLatestForEachUser()).toRankingEntries(
            Participants(participantRepository.findAll()),
            Regions(regionRepository.findAll()),
        )
        val snapshots = entries.toSnapshots(origin(), season.deliveryStart(), computedAt)
        pruneSnapshots(shares)
        resultSnapshotRepository.saveAll(snapshots.toList())
    }

    fun findLatestSnapshot(participantId: UUID): ResultSnapshot? =
        resultSnapshotRepository.findFirstByUserIdOrderByComputedAtDesc(participantId.toString())

    fun findLatestResult(participantId: UUID): RankedResult {
        val snapshot = findLatestSnapshot(participantId) ?: throw SantaCutException(ErrorCode.RESULT_NOT_FOUND)
        val assessment = assessmentRepository.findById(snapshot.assessmentId).orElse(null)
            ?: throw SantaCutException(ErrorCode.RESULT_NOT_FOUND)
        return RankedResult(snapshot, assessment)
    }

    /** 성공한 분석 작업의 평가와 그 평가가 처음 반영된 최신 순위. */
    fun findResultOfAssessment(assessmentId: String): RankedResult {
        val assessment = assessmentRepository.findById(assessmentId).orElse(null)
            ?: throw SantaCutException(ErrorCode.ANALYSIS_RESULT_MISSING)
        val snapshot = resultSnapshotRepository.findFirstByAssessmentIdOrderByComputedAtDesc(assessmentId)
            ?: throw SantaCutException(ErrorCode.ANALYSIS_RESULT_MISSING)
        return RankedResult(snapshot, assessment)
    }

    /** 가장 최근 순위 계산 결과의 한 페이지. */
    fun findRankingPage(page: RankingPage): RankingRows {
        val latest = resultSnapshotRepository.findFirstByOrderByComputedAtDesc() ?: return RankingRows.empty()
        return RankingRows(resultSnapshotRepository.findRankingRows(latest.computedAt, page.offset, page.size))
    }

    /**
     * 현재 순위와 공유 링크가 붙잡은 스냅숏만 남긴다.
     * 지우지 않으면 분석 한 번마다 전체 순위가 복사되어 저장 공간이 제곱으로 늘어난다.
     */
    private fun pruneSnapshots(shares: Shares) {
        val stale = ResultSnapshots(resultSnapshotRepository.findAll()).excluding(shares.snapshotIds())
        if (!stale.isEmpty()) resultSnapshotRepository.deleteAllInBatch(stale.toList())
    }

    private fun origin(): Coordinate = Coordinate(appProperties.originLat, appProperties.originLon)
}
