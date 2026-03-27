@file:OptIn(ExperimentalTime::class)

package com.au.swarin.selective_tests.repository

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.ExperimentalTime

@Table("question")
data class Question(
    @Id
    val id: UUID? = null,
    val text: String,
    val images: Images? = null,
    val options: Options,
    val answer: String,
    val tags: JsonNode? = null,
    @Column("created_at")
    val createdAt: LocalDateTime,
)

@Table("question_paper")
data class QuestionPaper(
    @Id
    val id: UUID,
    val paper: Paper,
    @Column("created_at")
    val createdAt: LocalDateTime,
)

data class Paper(
    val questions: List<PaperQuestion> = emptyList(),
)

data class PaperQuestion(
    val questionNo: Int,
    val questionId: UUID,
)

@Table("test")
data class Test(
    @Id
    val id: UUID,
    val subject: String,
    @Column("duration_mins")
    val durationMins: Int,
    val instructions: String?,
    @Column("question_paper_id")
    val questionPaperId: UUID,
    @Column("created_at")
    val createdAt: LocalDateTime,
)

@Table("test_result")
data class TestResult(
    @Id
    val id: UUID,
    @Column("test_id")
    val testId: UUID,
    val score: BigDecimal,
    @Column("created_at")
    val createdAt: LocalDateTime,
)

@Table("attempt")
data class Attempt(
    @Id
    val id: UUID? = null,
    @Column("test_id")
    val testId: UUID,
    val answers: JsonNode? = null,
    val status: Status,
    @Column("started_at")
    val startedAt: LocalDateTime? = null,
    @Column("submitted_at")
    val submittedAt: LocalDateTime? = null
) {
    enum class Status {
        IN_PROGRESS,
        COMPLETED,
    }
}

@Table("tag")
data class Tag(
    @Id
    val id: UUID? = null,
    val entity: String,
    val key: String,
    val value: String,
    @Column("created_at")
    val createdAt: LocalDateTime,
)

data class Image(val name: String, val index: Int = 1, val text: String? = null)
data class Images(val images: List<Image>)

data class Option(val label: String, val text: String? = null, val image: Images? = null)
data class Options(val options: List<Option>)
