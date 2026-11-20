package com.santacut.infrastructure.roast.repository

import com.santacut.domain.roast.model.Roast
import org.springframework.data.jpa.repository.JpaRepository

interface RoastRepository : JpaRepository<Roast, String> {
    fun findByAssessmentId(assessmentId: String): Roast?
}
