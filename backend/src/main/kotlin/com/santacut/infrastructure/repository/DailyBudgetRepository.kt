package com.santacut.infrastructure.repository

import com.santacut.domain.model.DailyBudget
import org.springframework.data.jpa.repository.JpaRepository

interface DailyBudgetRepository : JpaRepository<DailyBudget, String>
