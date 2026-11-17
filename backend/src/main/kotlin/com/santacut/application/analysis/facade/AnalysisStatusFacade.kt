package com.santacut.application.analysis.facade

import com.santacut.application.analysis.command.AnalysisJobCommand
import com.santacut.application.analysis.command.LatestAnalysisCommand
import com.santacut.application.analysis.command.OpenAnalysisStreamCommand
import com.santacut.application.analysis.result.AnalysisStatusChangeResult
import com.santacut.application.analysis.result.AnalysisStatusResult
import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisJobChange
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.ranking.service.RankingService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Clock
import java.util.UUID

/** 분석 작업의 진행 단계를 조회하고, 작업 행이 바뀌면 진행 상황 스트림에 새 상태를 알린다. */
@Facade
class AnalysisStatusFacade(
    private val analysisJobService: AnalysisJobService,
    private val rankingService: RankingService,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(AnalysisStatusFacade::class.java)

    /** 작업 하나의 현재 진행 단계. 화면은 끝날 때까지 이 조회를 반복한다. */
    @Transactional(readOnly = true)
    fun getStatus(command: AnalysisJobCommand): AnalysisStatusResult {
        val job = analysisJobService.getOwned(command.jobId, command.participantId)
        return AnalysisStatusResult.from(job, rankingService.progressOf(job, command.participantId))
    }

    /** 참가자가 가장 최근에 낸 작업의 진행 단계. 재방문 때 이어 볼 작업을 찾는다. */
    @Transactional(readOnly = true)
    fun getLatestStatus(command: LatestAnalysisCommand): AnalysisStatusResult {
        val job = analysisJobService.getLatestOwned(command.participantId)
        return AnalysisStatusResult.from(job, rankingService.progressOf(job, command.participantId))
    }

    /**
     * 진행 상황 스트림을 하나 더 열 수 있는지 확인한다. 상한에 걸리면 ANALYSIS_STREAM_LIMIT(503)이고, 화면은 폴링으로 바꾼다.
     * 연결하자마자 보낼 현재 상태는 스트림을 등록한 뒤 [getStatus]로 읽는다.
     */
    fun verifyStreamOpenable(command: OpenAnalysisStreamCommand) {
        analysisJobService.verifyStreamOpenable(command.openStreams, command.replacing, clock.instant())
    }

    /**
     * 작업 행을 저장한 트랜잭션이 커밋된 뒤 응답용 새 상태를 만들어 진행 상황 스트림에 알린다.
     * 커밋 뒤 단계라 원래 트랜잭션 자원으로 읽기만 한다. 실패해도 작업 흐름을 막지 않도록 로그만 남긴다.
     */
    @TransactionalEventListener(fallbackExecution = true)
    fun announceStatusChange(change: AnalysisJobChange) {
        try {
            val job = analysisJobService.find(change.jobId) ?: return
            val participantId = UUID.fromString(job.userId)
            val status = AnalysisStatusResult.from(job, rankingService.progressOf(job, participantId))
            eventPublisher.publishEvent(AnalysisStatusChangeResult(participantId, status))
        } catch (exception: RuntimeException) {
            logger.warn("분석 작업 {} 상태 알림 실패: {}", change.jobId, exception.javaClass.simpleName)
        }
    }
}
