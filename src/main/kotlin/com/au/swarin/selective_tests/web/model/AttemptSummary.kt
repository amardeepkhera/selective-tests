package com.au.swarin.selective_tests.web.model

import java.time.LocalDateTime
import java.util.UUID

data class AttemptSummary(
    val attemptId: UUID? = null,
    val startedAt: LocalDateTime? = null,
    private val answers: MutableMap<Int, String> = mutableMapOf(),
) {
    fun addAnswer(questionNo: Int, answer: String) {
        answers[questionNo] = answer
    }
}
