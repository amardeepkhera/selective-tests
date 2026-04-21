package com.au.swarin.selective_tests.web.model

import java.util.UUID

data class QuestionPaperFormState(
    var paperName: String = "",
    val selectedQuestionIds: MutableList<UUID> = mutableListOf(),
)
