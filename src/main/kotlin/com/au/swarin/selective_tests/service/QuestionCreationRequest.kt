package com.au.swarin.selective_tests.service

data class QuestionCreationRequest(
    val text: String,
    val imagesJson: String,
    val optionsJson: String,
    val tagsJson: String,
    val answer: String,
)
