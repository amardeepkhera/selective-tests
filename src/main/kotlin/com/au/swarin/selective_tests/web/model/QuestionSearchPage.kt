package com.au.swarin.selective_tests.web.model

data class QuestionSearchPage(
    val questions: List<QuestionListItem>,
    val page: Int,
    val size: Int,
    val hasMore: Boolean,
)
