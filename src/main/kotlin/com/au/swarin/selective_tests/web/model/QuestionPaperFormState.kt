package com.au.swarin.selective_tests.web.model

import java.util.UUID

data class QuestionPaperFormState(
    var paperName: String = "",
    var tagsJson: String = "",
    val selectedQuestionIds: MutableSet<UUID> = mutableSetOf(),
    var questionPaperId: UUID? = null,
)
