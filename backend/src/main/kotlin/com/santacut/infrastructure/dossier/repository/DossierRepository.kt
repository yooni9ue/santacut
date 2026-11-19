package com.santacut.infrastructure.dossier.repository

import com.santacut.domain.dossier.model.Dossier
import org.springframework.data.jpa.repository.JpaRepository

interface DossierRepository : JpaRepository<Dossier, String> {
    fun findByAssessmentId(assessmentId: String): Dossier?

    fun existsByAssessmentId(assessmentId: String): Boolean
}
