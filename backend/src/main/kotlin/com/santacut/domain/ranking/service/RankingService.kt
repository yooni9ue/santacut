package com.santacut.domain.ranking.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisProgress
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.Assessments
import com.santacut.domain.global.config.AppProperties
import com.santacut.domain.global.config.RankingProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.Coordinate
import com.santacut.domain.participant.model.Participants
import com.santacut.domain.participant.model.Regions
import com.santacut.domain.ranking.model.FirstAssessments
import com.santacut.domain.ranking.model.RankedResult
import com.santacut.domain.ranking.model.RankedAssessmentTimes
import com.santacut.domain.ranking.model.RankingBoard
import com.santacut.domain.ranking.model.RankingBoardCache
import com.santacut.domain.ranking.model.RankingCursor
import com.santacut.domain.ranking.model.RankingRefresh
import com.santacut.domain.ranking.model.RankingRows
import com.santacut.domain.ranking.model.RankingStage
import com.santacut.domain.ranking.model.ResultPopulation
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.ranking.model.Season
import com.santacut.infrastructure.analysis.repository.AssessmentRepository
import com.santacut.infrastructure.participant.repository.ParticipantRepository
import com.santacut.infrastructure.participant.repository.RegionRepository
import com.santacut.infrastructure.ranking.repository.RankingRefreshRepository
import com.santacut.infrastructure.ranking.repository.ResultPopulationRepository
import com.santacut.infrastructure.ranking.repository.ResultSnapshotRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class RankingService(
    private val assessmentRepository: AssessmentRepository,
    private val participantRepository: ParticipantRepository,
    private val regionRepository: RegionRepository,
    private val resultSnapshotRepository: ResultSnapshotRepository,
    private val resultPopulationRepository: ResultPopulationRepository,
    private val rankingRefreshRepository: RankingRefreshRepository,
    private val appProperties: AppProperties,
    private val rankingProperties: RankingProperties,
) {
    private val firstPageCache = RankingBoardCache(rankingProperties.firstPageTtl)

    /**
     * 순위에 영향을 주는 변경(심사 성공, 지역 변경, 참가자 삭제)이 있었다고 표시만 한다. 같은 트랜잭션에서 커밋된다.
     * 실제 계산은 예약 작업이 [rebuildIfRequested]로 모아서 한다.
     */
    fun requestRebuild() {
        if (rankingRefreshRepository.markRequested(RankingRefresh.ID) == 0) {
            rankingRefreshRepository.save(RankingRefresh.requested())
        }
    }

    /**
     * 참가자를 지운 뒤 처리. 시즌 마감 전이면 다시 계산을 요청하고, 마감 뒤에는 확정 순위를 그대로 두고 분포만 다시 센다.
     */
    fun forgetDeletedParticipant(now: Instant) {
        if (season().isClosedAt(now)) refreshPopulation() else requestRebuild()
    }

    /**
     * 요청이 쌓여 있으면 순위를 다시 계산하고, 시작할 때 읽은 요청 판까지 반영했다고 표시한다.
     * @return 순위 스냅숏을 새로 만들었으면 참
     */
    fun rebuildIfRequested(now: Instant): Boolean {
        val refresh = rankingRefreshRepository.findById(RankingRefresh.ID).orElse(null) ?: return false
        if (!refresh.isDirty()) return false
        val rebuilt = rebuild(now)
        rankingRefreshRepository.markRebuilt(RankingRefresh.ID, refresh.requestedVersion)
        return rebuilt
    }

    /**
     * 참가자별 최신 평가로 순위와 배송 예정 시각을 새로 계산해 저장한다. 동시에 부르지 않는 것은 호출하는 facade가 맡는다.
     * 결과 화면이 쓰는 장난기 기준값과 유형 분포([ResultPopulation])도 같은 평가로 함께 센다.
     * 시즌 마감 뒤에 만든 스냅숏이 이미 있으면 확정 순위이므로 분포만 다시 센다.
     * 공유 링크가 붙잡은 스냅숏은 지우지 않는다. 지우기는 한 문장, 저장은 묶음 INSERT라 참가자가 많아도 DB 연결을 오래 쥐지 않는다.
     * @return 순위 스냅숏을 새로 만들었으면 참
     */
    fun rebuild(now: Instant): Boolean {
        val latest = Assessments(assessmentRepository.findLatestForEachUser())
        resultPopulationRepository.save(ResultPopulation.of(latest))
        if (isFinalized()) return false
        val entries = latest.toRankingEntries(
            Participants(participantRepository.findAll()),
            Regions(regionRepository.findAll()),
            FirstAssessments(assessmentRepository.findFirstForEachUser()),
        )
        val computedAt = now.truncatedTo(ChronoUnit.MILLIS)
        val snapshots = entries.toSnapshots(origin(), season().deliveryStart(), computedAt, rankingProperties.deliveryWindowDays)
        pruneSnapshots()
        resultSnapshotRepository.saveAll(snapshots.toList())
        firstPageCache.invalidate()
        return true
    }

    /** 가장 최근 순위 계산 시각. 순위를 계산한 적이 없으면 null이다. */
    fun latestComputedAt(): Instant? = resultSnapshotRepository.findFirstByOrderByComputedAtDesc()?.computedAt

    /**
     * 장난기 기준값과 유형 분포를 지금 평가로 다시 센다. 앱이 뜰 때 불러, 시즌 마감 뒤라도 저장한 행이 늘 있게 하고
     * 유형 규칙이 바뀐 배포 뒤의 분포도 맞춘다.
     */
    fun refreshPopulation() {
        resultPopulationRepository.save(ResultPopulation.of(Assessments(assessmentRepository.findLatestForEachUser())))
    }

    fun findLatestSnapshot(participantId: UUID): ResultSnapshot? =
        resultSnapshotRepository.findFirstByUserIdOrderByComputedAtDesc(participantId.toString())

    /** 참가자의 최신 순위 스냅숏. 아직 순위에 오른 결과가 없으면 [ErrorCode.NO_RANKED_RESULT]다. */
    fun getLatestSnapshot(participantId: UUID): ResultSnapshot =
        findLatestSnapshot(participantId) ?: throw SantaCutException(ErrorCode.NO_RANKED_RESULT)

    /** 참가자([participantId])가 낸 작업의 응답용 진행 단계. 성공한 작업은 최신 순위에 반영됐는지까지 본다([AnalysisJob.progress]). */
    fun progressOf(job: AnalysisJob, participantId: UUID): AnalysisProgress = job.progress(findRankedAssessmentTime(participantId))

    /**
     * 참가자의 최신 순위 스냅숏이 담은 평가를 만든 시각. 이 시각 이후에 끝난 심사는 아직 순위에 반영되지 않았다.
     * 순위 스냅숏이 없으면 null이다.
     */
    fun findRankedAssessmentTime(participantId: UUID): Instant? =
        resultSnapshotRepository.findRankedAssessmentTime(participantId.toString())

    /** [userIds] 참가자마다 [findRankedAssessmentTime]을 한 번의 조회로 읽는다. */
    fun findRankedAssessmentTimes(userIds: Set<String>): RankedAssessmentTimes =
        RankedAssessmentTimes(resultSnapshotRepository.findRankedAssessmentTimes(userIds))

    /**
     * 참가자의 최신 결과. 점수와 순위를 어떻게 묶을지는 [RankedResult.of]가 정한다.
     * 평가가 하나도 없으면 [ErrorCode.RESULT_NOT_FOUND]다.
     */
    fun findLatestResult(participantId: UUID): RankedResult {
        val latest = assessmentRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(participantId.toString())
            ?: throw SantaCutException(ErrorCode.RESULT_NOT_FOUND)
        return RankedResult.of(latest, findLatestSnapshot(participantId)) { assessmentId ->
            assessmentRepository.findById(assessmentId).orElse(null)
        }
    }

    /**
     * [assessment]와 그 순위 [snapshot]의 재미용 꾸밈 값(유형, 항목 막대, 상위 비율, 배지, 한마디).
     * 장난기 기준값과 유형 비율은 순위 계산과 앱 시작 때 저장한 값([refreshPopulation])만 읽고 평가 전체를 세지 않는다.
     * 저장한 값이 없으면(평가가 하나도 없던 때) 빈 분포로 본다.
     * 상위 비율은 그 순위 계산의 참가자 수로 낸다.
     */
    fun findProfile(assessment: Assessment, snapshot: ResultSnapshot?): ResultProfile {
        val participantCount = snapshot?.let { resultSnapshotRepository.countByComputedAt(it.computedAt) } ?: 0
        val population = resultPopulationRepository.findById(ResultPopulation.LATEST_ID).orElse(null) ?: ResultPopulation.EMPTY
        return ResultProfile.of(assessment, snapshot, population, participantCount)
    }

    /**
     * 가장 최근 순위 계산 결과의 한 페이지와 참가자 수, 계산 시각, 확정 상태.
     * cursor가 다른 순위 계산의 것이면 [ErrorCode.RANKING_SNAPSHOT_CHANGED]다. 첫 페이지는 [RankingBoardCache]에 잠깐 둔다.
     */
    fun findRankingBoard(cursor: RankingCursor, now: Instant): RankingBoard {
        val season = season()
        val computedAt = latestComputedAt() ?: return RankingBoard.empty(season, now)
        cursor.verifySnapshot(computedAt)
        if (!cursor.isFirstPage()) return readRankingBoard(season, computedAt, cursor, now)
        return firstPageCache.find(computedAt, season.stageAt(now), now)
            ?: readRankingBoard(season, computedAt, cursor, now).also { board -> firstPageCache.store(board, now) }
    }

    /** 시즌이 마감됐으면 [ErrorCode.SEASON_FROZEN]이다. 마감 뒤에는 순위에 영향을 주는 변경을 받지 않는다. */
    fun verifySeasonOpenAt(now: Instant) = season().verifyOpenAt(now)

    /** 지금 순위가 미리보기인지 확정인지. */
    fun stageAt(now: Instant): RankingStage = season().stageAt(now)

    /**
     * 현재 순위와 공유 링크가 붙잡은 스냅숏만 남긴다.
     * 지우지 않으면 분석 한 번마다 전체 순위가 복사되어 저장 공간이 제곱으로 늘어난다.
     */
    private fun pruneSnapshots() {
        resultSnapshotRepository.deleteUnshared()
    }

    private fun readRankingBoard(season: Season, computedAt: Instant, cursor: RankingCursor, now: Instant): RankingBoard {
        val fetched = RankingRows(
            resultSnapshotRepository.findRankingRows(computedAt, cursor.afterSequence, cursor.fetchSize),
        )
        val participantCount = resultSnapshotRepository.countByComputedAt(computedAt)
        return RankingBoard.of(season, now, participantCount, computedAt, fetched, cursor)
    }

    /** 시즌 마감 뒤에 만든 순위가 이미 있어 배송 계획이 확정됐는지. */
    private fun isFinalized(): Boolean = latestComputedAt()?.let(season()::isClosedAt) == true

    private fun season(): Season = Season(appProperties.seasonYear)

    private fun origin(): Coordinate = Coordinate(appProperties.originLat, appProperties.originLon)
}
