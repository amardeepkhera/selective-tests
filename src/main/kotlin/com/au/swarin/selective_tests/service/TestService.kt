package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.repository.Test
import com.au.swarin.selective_tests.repository.TestRepository
import com.au.swarin.selective_tests.repository.QuestionPaperRepository
import com.au.swarin.selective_tests.web.model.TestSummary
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
class TestService(
    private val testRepository: TestRepository,
    private val questionPaperRepository: QuestionPaperRepository,
) {
    fun getAllTests(): List<Test> = testRepository.findAll()

    fun getTestById(testId: UUID): Test? = testRepository.findById(testId).orElse(null)

    fun getTestSummary(testId: UUID): TestSummary? = testRepository.findById(testId).getOrNull()?.let { test ->
        val questionPaper = questionPaperRepository.findById(test.questionPaperId).getOrNull() ?: return null
        TestSummary(
            testId = test.id,
            durationMins = test.durationMins,
            questionMap = questionPaper.paper.questions.associateBy { it.questionNo!! },
        )
    }

}

