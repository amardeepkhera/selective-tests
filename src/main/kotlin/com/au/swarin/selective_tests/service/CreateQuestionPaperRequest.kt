package com.au.swarin.selective_tests.service

import java.util.UUID

data class CreateQuestionPaperRequest(
    val name: String,
    val numberedQuestions: Map<Int, UUID> = emptyMap(),
    val questions: Set<UUID> = emptySet(),
    val tagsJson: String? = null,
)
