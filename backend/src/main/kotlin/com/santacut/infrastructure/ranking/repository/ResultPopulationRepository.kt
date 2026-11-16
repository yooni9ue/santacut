package com.santacut.infrastructure.ranking.repository

import com.santacut.domain.ranking.model.ResultPopulation
import org.springframework.data.jpa.repository.JpaRepository

interface ResultPopulationRepository : JpaRepository<ResultPopulation, Int>
