package com.au.swarin.selective_tests.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface QuestionRepository : ListCrudRepository<Question, UUID> {

    @Query(
        """
        SELECT q.*
        FROM selective.question q
        WHERE q.tags IS NOT NULL
          AND EXISTS (
              SELECT 1
              FROM jsonb_array_elements_text(q.tags) AS tag_id
              WHERE tag_id::uuid IN (:tagIds)
          )
        ORDER BY q.created_at DESC, q.id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findAllByAnyTagIdIn(
        @Param("tagIds") tagIds: Set<UUID>,
        @Param("limit") limit: Int,
        @Param("offset") offset: Long,
    ): List<Question>

    @Query(
        """
        SELECT DISTINCT tag_id::uuid
        FROM selective.question q
        CROSS JOIN LATERAL jsonb_array_elements_text(q.tags) AS tag_id
        WHERE q.id IN (:questionIds)
        """
    )
    fun getTags(@Param("questionIds") questionIds: Set<UUID>): Set<UUID>
}
