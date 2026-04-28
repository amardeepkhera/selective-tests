package com.au.swarin.selective_tests.repository

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface QuestionPaperRepository : ListCrudRepository<QuestionPaper, UUID>{

    @Modifying
    @Query(
        """
        UPDATE selective.question_paper
        SET tags = :tags
        WHERE id = :questionPaperId
        """
    )
    fun addTags(
        @Param("questionPaperId") questionPaperId: UUID,
        @Param("tags") tags: JsonNode,
    )
    @Modifying
    @Query("UPDATE selective.question_paper SET status = :status WHERE id = :questionPaperId")
    fun updateStatus(questionPaperId: UUID, status: String)
}
