@file:OptIn(ExperimentalTime::class)

package com.au.swarin.selective_tests.web.model

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.ExperimentalTime

data class TestState(
    private var questionNo: Int = 1,
    private val testSummary: TestSummary,
    private val attemptSummary: AttemptSummary = AttemptSummary(),
) {
    fun testId() = testSummary.testId

    fun currentQuestionNo() = questionNo

    fun currentQuestionId() = currentQuestionId(questionNo)

    private fun currentQuestionId(questionNo: Int) =
        testSummary.questionMap[questionNo]?.questionId ?: throw NoSuchElementException("No value present")

    fun nextQuestion() {
        this.questionNo = questionNo + 1
    }

    fun previousQuestion() {
        this.questionNo = questionNo - 1
    }

    fun totalDurationInSecs() = testSummary.durationMins.times(60)
        .minus(Duration.between(attemptSummary.startedAt, LocalDateTime.now(ZoneOffset.UTC)).seconds)

    fun totalQuestions() = testSummary.questionMap.size

    fun hasPrevious() = questionNo > 1

    fun hasNext() = questionNo < totalQuestions()

    fun addAnswer(answer: String) {
        attemptSummary.addAnswer(questionNo, answer)
    }
}
