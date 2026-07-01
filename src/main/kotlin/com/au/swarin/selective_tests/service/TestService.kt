package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.nowAtUTCAndTruncatedToMins
import com.au.swarin.selective_tests.repository.QuestionPaperRepository
import com.au.swarin.selective_tests.repository.Test
import com.au.swarin.selective_tests.repository.TestDetails
import com.au.swarin.selective_tests.repository.TestListItem
import com.au.swarin.selective_tests.repository.TestRepository
import com.au.swarin.selective_tests.web.model.TestSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
class TestService(
    private val testRepository: TestRepository,
    private val questionPaperRepository: QuestionPaperRepository
) {
    fun getAllTests(): List<TestListItem> = testRepository.findAllSummaries()

    fun getTestById(testId: UUID): TestDetails? = testRepository.findDetailsById(testId)


    @Transactional
    fun createTest(
        questionPaperId: UUID,
        name: String,
        subjectId: UUID,
        durationMins: Int,
        instructions: String?,
    ): Test? {

        return testRepository.save(
            Test(
                name = name,
                subjectId = subjectId,
                durationMins = durationMins,
                instructions = instructions?.takeIf { it.isNotBlank() },
                questionPaperId = questionPaperId,
                createdAt = nowAtUTCAndTruncatedToMins(),
            )
        )
    }

    fun getTestSummary(testId: UUID): TestSummary? =
        testRepository.findById(testId).getOrNull()?.let { test ->
            val questionPaper = questionPaperRepository.findById(test.questionPaperId).getOrNull() ?: return null
            TestSummary(
                testId = test.id!!,
                durationMins = test.durationMins,
                questionMap = questionPaper.paper.questions.associateBy { it.questionNo!! },
            )
        }

}
