package com.au.swarin.selective_tests.web

import com.au.swarin.selective_tests.service.AttemptService
import com.au.swarin.selective_tests.service.QuestionService
import com.au.swarin.selective_tests.service.TestService
import com.au.swarin.selective_tests.web.model.AttemptSummary
import com.au.swarin.selective_tests.web.model.TestState
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.SessionAttribute
import org.springframework.web.servlet.View
import java.util.UUID
import kotlin.time.ExperimentalTime

private const val TEST_STATE_SESSION = "testState"

@OptIn(ExperimentalTime::class)
@Controller
class TestController(
    private val testService: TestService,
    private val questionService: QuestionService,
    private val attemptService: AttemptService,
) {
    @GetMapping("/tests")
    fun tests(model: Model): String {
        model.addAttribute("tests", testService.getAllTests())
        return "tests"
    }

    @GetMapping("/tests/{testId}")
    fun testById(
        @PathVariable testId: UUID,
        model: Model,
    ): String {
        val test = testService.getTestById(testId)
        if (test == null) {
            model.addAttribute("errorMessage", "Test not found.")
        } else {
            model.addAttribute("test", test)
        }
        return "test-details"
    }

    @OptIn(ExperimentalTime::class)
    @PostMapping("/tests/{testId}")
    fun initTest(
        @PathVariable testId: UUID,
        request: HttpServletRequest
    ): String {
        val testSummary = testService.getTestSummary(testId) ?: return "redirect:/tests"

        val testState = TestState(
            questionNo = 1,
            testSummary = testSummary
        )
        request.session.setAttribute(TEST_STATE_SESSION, testState)
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.TEMPORARY_REDIRECT)
        return "redirect:/test"
    }


    @PostMapping("/test")
    fun startTest(
        request: HttpServletRequest,
        @SessionAttribute(TEST_STATE_SESSION) testState: TestState,
        model: Model
    ): String {
        val attempt = attemptService.createAttempt(testState.testId())

        return renderTest(
            request,
            testState.copy(
                attemptSummary = AttemptSummary(
                    attemptId = attempt.id,
                    startedAt = attempt.startedAt!!,
                ),
            ),
            model,
        )
    }

    @PostMapping("/test/submit")
    fun submitAnswer(
        request: HttpServletRequest,
        @SessionAttribute(TEST_STATE_SESSION) testState: TestState,
        @RequestParam selectedAnswer: String,
        model: Model
    ): String {
        testState.addAnswer(selectedAnswer)
        testState.nextQuestion()
        return renderTest(request, testState, model)
    }


    @PostMapping("/test/next")
    fun nextQuestion(
        request: HttpServletRequest,
        @SessionAttribute(TEST_STATE_SESSION) testState: TestState,
        model: Model
    ): String {
        testState.nextQuestion()
        return renderTest(request, testState, model)
    }

    @PostMapping("/test/previous")
    fun previousQuestion(
        request: HttpServletRequest,
        @SessionAttribute(TEST_STATE_SESSION) testState: TestState,
        model: Model
    ): String {
        testState.previousQuestion()
        return renderTest(request, testState, model)
    }

    private fun renderTest(
        request: HttpServletRequest,
        @SessionAttribute(TEST_STATE_SESSION) testState: TestState,
        model: Model
    ): String {
        request.session.setAttribute(TEST_STATE_SESSION, testState)
        val question = questionService.getQuestion(testState.currentQuestionId())
        model.addAttribute("question", question)
        model.addAttribute("testState", testState)
        return "test"
    }
}
