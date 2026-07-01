package com.au.swarin.selective_tests.web.model

import java.io.Serializable
import java.util.UUID

data class TestFormState(
    val questionPaperId: UUID,
    val questionCount: Int,
    var name: String = "",
    var subjectId: UUID? = null,
    var durationMins: Int = 0,
    var instructions: String = "",
): Serializable
