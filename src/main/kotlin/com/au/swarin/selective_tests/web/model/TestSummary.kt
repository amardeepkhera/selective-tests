package com.au.swarin.selective_tests.web.model

import com.au.swarin.selective_tests.repository.PaperQuestion
import java.util.UUID

data class TestSummary(
    val testId: UUID,
    val durationMins: Int,
    val questionMap: Map<Int, PaperQuestion>,
)
