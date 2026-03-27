package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.repository.Attempt
import com.au.swarin.selective_tests.repository.AttemptRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class AttemptService(private val attemptRepository: AttemptRepository) {

    fun createAttempt(testId: UUID): Attempt {
        val attempt = Attempt(
            testId = testId,
            status = Attempt.Status.IN_PROGRESS,
            startedAt = LocalDateTime.now(UTC)
        )
        return attemptRepository.save(attempt)
    }

}