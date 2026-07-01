package com.au.swarin.selective_tests.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface TestRepository : ListCrudRepository<Test, UUID> {
    @Query(
        """
        SELECT t.id,
               t.name,
               s.name AS subject,
               t.duration_mins
        FROM selective.test t
        JOIN selective.subject s ON s.id = t.subject_id
        ORDER BY t.created_at DESC, t.id DESC
        """
    )
    fun findAllSummaries(): List<TestListItem>

    @Query(
        """
        SELECT t.id,
               t.name,
               s.name AS subject,
               t.duration_mins,
               t.instructions,
               t.question_paper_id,
               qp.paper ->> 'name' AS question_paper_name,
               t.created_at
        FROM selective.test t
        JOIN selective.subject s ON s.id = t.subject_id
        JOIN selective.question_paper qp ON qp.id = t.question_paper_id
        WHERE t.id = :testId
        """
    )
    fun findDetailsById(@Param("testId") testId: UUID): TestDetails?
}
