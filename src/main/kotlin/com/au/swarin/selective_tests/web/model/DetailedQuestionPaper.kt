package com.au.swarin.selective_tests.web.model

data class DetailedQuestionPaper(
    val name:String,
    val questions: List<QuestionListItem>,
    val uniqueTags: Set<String> = emptySet(),
    val isFinal: Boolean
)
