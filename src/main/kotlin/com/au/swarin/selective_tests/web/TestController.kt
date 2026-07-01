package com.au.swarin.selective_tests.web

import com.au.swarin.selective_tests.service.AttemptService
import com.au.swarin.selective_tests.service.QuestionService
import com.au.swarin.selective_tests.service.SubjectService
import com.au.swarin.selective_tests.service.TestService
import com.au.swarin.selective_tests.web.model.AttemptSummary
import com.au.swarin.selective_tests.web.model.TestFormState
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.UUID
import kotlin.time.ExperimentalTime

private const val TEST_STATE_SESSION = "testState"
private const val TEST_FORM_SESSION = "test_form"

@OptIn(ExperimentalTime::class)
@Controller
class TestController(
    private val testService: TestService,
    private val questionService: QuestionService,
    private val attemptService: AttemptService,
    private val subjectService: SubjectService,
) {
    @PostMapping("/tests/new")
    fun newTest(
        @RequestParam questionPaperId: UUID,
        @RequestParam questionCount: Int,
        request: HttpServletRequest
    ): String {
        request.session.setAttribute(
            TEST_FORM_SESSION,
            TestFormState(questionPaperId = questionPaperId, questionCount = questionCount)
        )
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.TEMPORARY_REDIRECT)
        return "redirect:/test-paper-form"
    }

    @PostMapping("/test-paper-form")
    fun testPaperForm(
        @SessionAttribute(TEST_FORM_SESSION) testPaperFormState: TestFormState,
        model: Model,
    ): String = renderTestPaperForm(model, testPaperFormState)

    @PostMapping("/tests")
    fun saveTestPaper(
        @RequestParam name: String,
        @RequestParam subjectId: UUID,
        @RequestParam durationMins: Int,
        @RequestParam(required = false) instructions: String,
        @SessionAttribute(TEST_FORM_SESSION) testPaperFormState: TestFormState,
        request: HttpServletRequest,
        redirectAttributes: RedirectAttributes,
    ): String {

        val savedTest = testService.createTest(
            questionPaperId = testPaperFormState.questionPaperId,
            name = name,
            subjectId = subjectId,
            durationMins = durationMins,
            instructions = instructions,
        )


        request.session.removeAttribute(TEST_FORM_SESSION)
        redirectAttributes.addFlashAttribute("successMessage", "Test ${savedTest!!.name} created.")
        return "redirect:/tests"
    }

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

    private fun renderTestPaperForm(
        model: Model,
        testPaperFormState: TestFormState,
    ): String {
        model.addAttribute("testForm", testPaperFormState)
        model.addAttribute("questionPaperId", testPaperFormState.questionPaperId)
        model.addAttribute("subjects", subjectService.getAllSubjects())
        return "test-paper-form"
    }

    private fun String.toUuidOrNull(): UUID? = runCatching(UUID::fromString).getOrNull()
}
