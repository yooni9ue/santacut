package com.santacut.infrastructure.share.repository

import com.santacut.domain.share.model.Share
import org.springframework.data.jpa.repository.JpaRepository

interface ShareRepository : JpaRepository<Share, String> {
    fun findByUserIdAndSnapshotId(userId: String, snapshotId: String): Share?

    fun findByToken(token: String): Share?

    fun findByTokenAndUserId(token: String, userId: String): Share?
}
