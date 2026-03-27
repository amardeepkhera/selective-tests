package com.au.swarin.selective_tests.repository

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AttemptRepository : ListCrudRepository<Attempt, UUID> {
    @Modifying
    @Query(
        """
        INSERT INTO selective.attempt (test_id, status)
        VALUES ( :testId, :status)
        """
    )
    fun save(
        @Param("testId") testId: UUID,
        @Param("status") status: Attempt.Status,
    ): Attempt
}
