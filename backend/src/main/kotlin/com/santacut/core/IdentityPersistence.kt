package com.santacut.core

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "participants")
class ParticipantEntity(
    @Id @Column(name = "id") var id: String = "",
    @Column(name = "nickname", nullable = false) var nickname: String = "",
    @Column(name = "nickname_key", nullable = false, unique = true) var nicknameKey: String = "",
    @Column(name = "region_code", nullable = false) var regionCode: String = "",
    @Column(name = "created_at", nullable = false) var createdAt: String = ""
)

@Entity
@Table(name = "regions")
class RegionEntity(
    @Id @Column(name = "code") var code: String = "",
    @Column(name = "name", nullable = false) var name: String = "",
    @Column(name = "latitude", nullable = false) var latitude: Double = 0.0,
    @Column(name = "longitude", nullable = false) var longitude: Double = 0.0
)

@Entity
@Table(name = "sessions")
class SessionEntity(
    @Id @Column(name = "token_hash") var tokenHash: String = "",
    @Column(name = "user_id", nullable = false) var userId: String = "",
    @Column(name = "expires_at", nullable = false) var expiresAt: String = ""
)

@Repository
interface ParticipantRepository : JpaRepository<ParticipantEntity, String> {
    fun existsByNicknameKey(nicknameKey: String): Boolean
}

@Repository
interface RegionRepository : JpaRepository<RegionEntity, String> {
    fun findAllByOrderByCodeAsc(): List<RegionEntity>
}

@Repository
interface SessionRepository : JpaRepository<SessionEntity, String>

@Entity
@Table(name = "assessments")
class AssessmentEntity(
    @Id @Column(name = "id") var id: String = "",
    @Column(name = "user_id", nullable = false) var userId: String = "",
    @Column(name = "source", nullable = false) var source: String = "",
    @Column(name = "score_hundredths", nullable = false) var scoreHundredths: Int = 0,
    @Column(name = "verdict", nullable = false) var verdict: String = "",
    @Column(name = "comment", nullable = false) var comment: String = "",
    @Column(name = "components", nullable = false) var components: String = "",
    @Column(name = "created_at", nullable = false) var createdAt: String = ""
)

@Entity
@Table(name = "analysis_jobs")
class AnalysisJobEntity(
    @Id @Column(name = "id") var id: String = "",
    @Column(name = "user_id", nullable = false) var userId: String = "",
    @Column(name = "idempotency_key", nullable = false) var idempotencyKey: String = "",
    @Column(name = "request_hash", nullable = false) var requestHash: String = "",
    @Column(name = "source", nullable = false) var source: String = "",
    @Column(name = "payload") var payload: String? = null,
    @Column(name = "status", nullable = false) var status: String = "QUEUED",
    @Column(name = "attempts", nullable = false) var attempts: Int = 0,
    @Column(name = "lease_token") var leaseToken: String? = null,
    @Column(name = "lease_until") var leaseUntil: String? = null,
    @Column(name = "error_code") var errorCode: String? = null,
    @Column(name = "assessment_id") var assessmentId: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: String = "",
    @Column(name = "finished_at") var finishedAt: String? = null
)

@Entity
@Table(name = "ai_daily_budget")
class DailyBudgetEntity(
    @Id @Column(name = "day") var day: String = "",
    @Column(name = "calls", nullable = false) var calls: Int = 0
)

@Entity
@Table(name = "result_snapshots")
class ResultSnapshotEntity(
    @Id @Column(name = "id") var id: String = "",
    @Column(name = "user_id", nullable = false) var userId: String = "",
    @Column(name = "assessment_id", nullable = false) var assessmentId: String = "",
    @Column(name = "rank", nullable = false) var rank: Int = 0,
    @Column(name = "delivery_sequence", nullable = false) var deliverySequence: Int = 0,
    @Column(name = "eta", nullable = false) var eta: String = "",
    @Column(name = "computed_at", nullable = false) var computedAt: String = ""
)

@Entity
@Table(name = "shares")
class ShareEntity(
    @Id @Column(name = "token") var token: String = "",
    @Column(name = "user_id", nullable = false) var userId: String = "",
    @Column(name = "snapshot_id", nullable = false) var snapshotId: String = "",
    @Column(name = "nickname", nullable = false) var nickname: String = "",
    @Column(name = "score_hundredths", nullable = false) var scoreHundredths: Int = 0,
    @Column(name = "rank", nullable = false) var rank: Int = 0,
    @Column(name = "eta", nullable = false) var eta: String = "",
    @Column(name = "verdict", nullable = false) var verdict: String = "",
    @Column(name = "comment", nullable = false) var comment: String = "",
    @Column(name = "computed_at", nullable = false) var computedAt: String = "",
    @Column(name = "revoked_at") var revokedAt: String? = null
)

interface AssessmentRepository : JpaRepository<AssessmentEntity, String> {
    fun findByUserIdOrderByCreatedAtDescIdDesc(userId: String): List<AssessmentEntity>

    @Query("""
        select a from AssessmentEntity a
        where not exists (
            select newer.id from AssessmentEntity newer
            where newer.userId = a.userId
              and (newer.createdAt > a.createdAt or (newer.createdAt = a.createdAt and newer.id > a.id))
        )
    """)
    fun findLatestForEachUser(): List<AssessmentEntity>
}

interface AnalysisJobRepository : JpaRepository<AnalysisJobEntity, String> {
    fun findByUserIdAndIdempotencyKey(userId: String, idempotencyKey: String): AnalysisJobEntity?
    fun countByUserIdAndCreatedAtAfter(userId: String, createdAt: String): Long
    fun findFirstByStatusOrderByCreatedAtAsc(status: String): AnalysisJobEntity?
    fun findFirstByStatusAndLeaseUntilBeforeOrderByCreatedAtAsc(status: String, leaseUntil: String): AnalysisJobEntity?
    fun findFirstByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(status: String, createdAt: String): AnalysisJobEntity?
    fun findFirstByStatusAndLeaseUntilBeforeAndAttemptsGreaterThanEqualOrderByCreatedAtAsc(status: String, leaseUntil: String, attempts: Int): AnalysisJobEntity?
    fun findFirstByStatusAndLeaseUntilBeforeAndAttemptsLessThanOrderByCreatedAtAsc(status: String, leaseUntil: String, attempts: Int): AnalysisJobEntity?
    fun findByIdAndUserId(id: String, userId: String): AnalysisJobEntity?
}

interface DailyBudgetRepository : JpaRepository<DailyBudgetEntity, String>

interface ResultSnapshotRepository : JpaRepository<ResultSnapshotEntity, String>, ResultSnapshotRepositoryCustom {
    fun findFirstByUserIdOrderByComputedAtDesc(userId: String): ResultSnapshotEntity?
    fun findFirstByOrderByComputedAtDesc(): ResultSnapshotEntity?
    fun findByComputedAtOrderByDeliverySequenceAsc(computedAt: String): List<ResultSnapshotEntity>
    fun findFirstByAssessmentIdOrderByComputedAtDesc(assessmentId: String): ResultSnapshotEntity?

}

interface ResultSnapshotRepositoryCustom {
    fun findRankingRows(computedAt: String, offset: Int, limit: Int): List<RankingRowProjection>
}

data class RankingRowProjection(
    val nickname: String,
    val rank: Int,
    val scoreHundredths: Int,
    val eta: String,
    val computedAt: String
)

class ResultSnapshotRepositoryImpl(private val entityManager: EntityManager) : ResultSnapshotRepositoryCustom {
    override fun findRankingRows(computedAt: String, offset: Int, limit: Int): List<RankingRowProjection> =
        entityManager.createQuery(
            """
                select new com.santacut.core.RankingRowProjection(
                    p.nickname, s.rank, a.scoreHundredths, s.eta, s.computedAt
                )
                from ResultSnapshotEntity s, ParticipantEntity p, AssessmentEntity a
                where p.id = s.userId and a.id = s.assessmentId and s.computedAt = :computedAt
                order by s.deliverySequence asc
            """.trimIndent(),
            RankingRowProjection::class.java
        )
            .setParameter("computedAt", computedAt)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .resultList
}

interface ShareRepository : JpaRepository<ShareEntity, String> {
    fun findByUserIdAndSnapshotIdAndRevokedAtIsNull(userId: String, snapshotId: String): ShareEntity?
    fun findAllByRevokedAtIsNull(): List<ShareEntity>
    fun deleteAllByRevokedAtIsNotNull()
    fun findByTokenAndRevokedAtIsNull(token: String): ShareEntity?
    fun findByTokenAndUserId(token: String, userId: String): ShareEntity?
}
