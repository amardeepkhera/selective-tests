package com.au.swarin.selective_tests.web.model

import com.au.swarin.selective_tests.repository.Paper
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

data class QuestionPaperListItem(
    val id: UUID?,
    val paper: Paper,
    val status: String,
    val tags: JsonNode? = null,
    val createdAt: LocalDateTime,
)
