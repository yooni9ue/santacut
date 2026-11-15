package com.santacut.infrastructure.repository

import com.santacut.domain.model.Share
import org.springframework.data.jpa.repository.JpaRepository

interface ShareRepository : JpaRepository<Share, String> {
    fun findByUserIdAndSnapshotId(userId: String, snapshotId: String): Share?

    fun findByToken(token: String): Share?

    fun findByTokenAndUserId(token: String, userId: String): Share?
}
