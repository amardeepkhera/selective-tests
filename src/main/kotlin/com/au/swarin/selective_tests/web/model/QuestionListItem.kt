package com.au.swarin.selective_tests.web.model

import com.au.swarin.selective_tests.repository.Images
import com.au.swarin.selective_tests.repository.Options
import java.time.LocalDateTime
import java.util.UUID

data class QuestionListItem(
    val id: UUID?,
    val text: String,
    val images: Images? = null,
    val options: Options? = null,
    val answer: String? = null,
    val createdAt: LocalDateTime? = null,
)
