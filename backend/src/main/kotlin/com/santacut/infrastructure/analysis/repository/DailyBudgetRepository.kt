package com.santacut.infrastructure.analysis.repository

import com.santacut.domain.analysis.model.DailyBudget
import org.springframework.data.jpa.repository.JpaRepository

interface DailyBudgetRepository : JpaRepository<DailyBudget, String>
