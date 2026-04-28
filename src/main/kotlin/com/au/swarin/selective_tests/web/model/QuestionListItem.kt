package com.au.swarin.selective_tests.web.model

import com.au.swarin.selective_tests.repository.Images
import com.au.swarin.selective_tests.repository.Options
import com.au.swarin.selective_tests.repository.Tag
import java.time.LocalDateTime
import java.util.UUID

data class QuestionListItem(
    val id: UUID?,
    val questionNo: Int? = null,
    val text: String,
    val images: Images? = null,
    val options: Options? = null,
    val answer: String? = null,
    val tags: Set<Tag> = emptySet(),
    val createdAt: LocalDateTime? = null,
)
