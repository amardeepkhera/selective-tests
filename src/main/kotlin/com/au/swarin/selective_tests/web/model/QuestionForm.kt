package com.au.swarin.selective_tests.web.model

data class QuestionForm(
    val text: String = "",
    val imagesJson: String = "",
    val optionsJson: String = "",
    val tagsJson: String = "",
    val answer: String = "",
)
