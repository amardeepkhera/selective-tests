package com.au.swarin.selective_tests.web

import com.au.swarin.selective_tests.service.CreateQuestionPaperRequest
import com.au.swarin.selective_tests.service.QuestionPaperService
import com.au.swarin.selective_tests.service.QuestionService
import com.au.swarin.selective_tests.web.model.QuestionPaperForm
import com.au.swarin.selective_tests.web.model.QuestionPaperFormState
import com.au.swarin.selective_tests.web.model.QuestionSearchPage
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.SessionAttribute
import org.springframework.web.servlet.View
import java.util.UUID

private const val QUESTION_PAPER_FORM_SESSION = "qp_form"

@Controller
class QuestionPaperController(
    private val questionPaperService: QuestionPaperService,
    private val questionService: QuestionService,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping("/question-papers")
    fun questionPapers(model: Model): String {
        model.addAttribute("questionPapers", questionPaperService.getAllQuestionPapers())
        return "question-papers"
    }

    @PostMapping("/question-papers/new")
    fun newQuestionPaper(request: HttpServletRequest): String {
        request.session.setAttribute(QUESTION_PAPER_FORM_SESSION, QuestionPaperFormState())
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.TEMPORARY_REDIRECT)
        return "redirect:/question-paper-form"
    }

    @GetMapping("/question-paper-form")
    fun questionPaperForm(
        @SessionAttribute(QUESTION_PAPER_FORM_SESSION) questionPaperFormState: QuestionPaperFormState,
        model: Model,
    ): String = renderQuestionPaperForm(model, questionPaperFormState)

    @PostMapping("/question-paper-form")
    fun showQuestionPaperForm(
        @SessionAttribute(QUESTION_PAPER_FORM_SESSION) questionPaperFormState: QuestionPaperFormState,
        model: Model
    ): String = renderQuestionPaperForm(model, questionPaperFormState)

    @PostMapping("/question-paper-form/save")
    fun saveQuestionPaper(
        @RequestParam paperName: String,
        @RequestParam(required = false) selectedQuestionIds: Set<UUID>?,
        @SessionAttribute(QUESTION_PAPER_FORM_SESSION) questionPaperFormState: QuestionPaperFormState,
        model: Model,
    ): String {
        val createQuestionPaperRequest =
            CreateQuestionPaperRequest(name = paperName, questions = selectedQuestionIds.orEmpty())
        questionPaperService.saveAsDraft(createQuestionPaperRequest)
//        updateQuestionPaperFormState(questionPaperFormState, paperName, selectedQuestionIds)
        model.addAttribute("successMessage", "Question paper draft captured.")
        return renderQuestionPaperForm(model, questionPaperFormState)
    }

    @PostMapping("/question-paper-form/next")
    fun nextQuestionPaper(
        @RequestParam paperName: String,
        @RequestParam(required = false) selectedQuestionIds: List<UUID>?,
        @SessionAttribute(QUESTION_PAPER_FORM_SESSION) questionPaperFormState: QuestionPaperFormState,
        model: Model,
    ): String {
        updateQuestionPaperFormState(questionPaperFormState, paperName, selectedQuestionIds)
        model.addAttribute("successMessage", "Question paper selection captured for the next step.")
        return renderQuestionPaperForm(model, questionPaperFormState)
    }

    @GetMapping("/question-paper-form/questions")
    @ResponseBody
    fun getQuestionsByTags(
        @RequestParam tagIds: Set<UUID>,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): QuestionSearchPage = questionService.getQuestions(tagIds, page, size)

    private fun updateQuestionPaperFormState(
        questionPaperFormState: QuestionPaperFormState,
        paperName: String,
        selectedQuestionIds: List<UUID>?,
    ) {
        questionPaperFormState.paperName = paperName.trim()
        questionPaperFormState.selectedQuestionIds.clear()
        questionPaperFormState.selectedQuestionIds.addAll(selectedQuestionIds.orEmpty())
    }

    private fun renderQuestionPaperForm(
        model: Model,
        questionPaperFormState: QuestionPaperFormState,
    ): String {
        model.addAttribute(
            "questionPaperForm",
            QuestionPaperForm(
                paperName = questionPaperFormState.paperName,
            ),
        )
        model.addAttribute("availableTagsJson", objectMapper.writeValueAsString(questionService.getAvailableTags()))
        return "question-paper-form"
    }
}
