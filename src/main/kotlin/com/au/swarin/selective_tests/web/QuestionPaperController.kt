package com.au.swarin.selective_tests.web

import com.au.swarin.selective_tests.service.CreateQuestionPaperRequest
import com.au.swarin.selective_tests.service.QuestionPaperService
import com.au.swarin.selective_tests.service.QuestionService
import com.au.swarin.selective_tests.service.TagHelper
import com.au.swarin.selective_tests.web.model.QuestionPaperForm
import com.au.swarin.selective_tests.web.model.QuestionPaperFormState
import com.au.swarin.selective_tests.web.model.QuestionSearchPage
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.SessionAttribute
import org.springframework.web.bind.support.SessionStatus
import org.springframework.web.servlet.View
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.UUID

private const val QUESTION_PAPER_FORM_SESSION = "qp_form"

@Controller
class QuestionPaperController(
    private val questionPaperService: QuestionPaperService,
    private val questionService: QuestionService,
    private val objectMapper: ObjectMapper,
    private val tagHelper: TagHelper,
) {

    @GetMapping("/question-papers")
    fun questionPapers(model: Model): String {
        model.addAttribute("questionPapers", questionPaperService.getAllQuestionPapers())
        return "question-papers"
    }

    @GetMapping("/question-papers/{questionPaperId}")
    fun questionPaperById(
        @PathVariable questionPaperId: UUID,
        model: Model,
    ): String {
        val questionPaper = questionPaperService.getQuestionPaperById(questionPaperId)
        if (questionPaper == null) {
            model.addAttribute("errorMessage", "Question paper not found.")
        } else {
            model.addAttribute("questionPaper", questionPaper)
        }
        return "question-paper-details"
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
        redirectAttributes: RedirectAttributes,
    ): String {
        val createQuestionPaperRequest =
            CreateQuestionPaperRequest(name = paperName, questions = selectedQuestionIds.orEmpty())
        questionPaperService.saveAsDraft(createQuestionPaperRequest)
        redirectAttributes.addFlashAttribute("successMessage", "Question paper $paperName saved as draft.")
        return "redirect:/question-papers"
    }

    @PostMapping("/question-paper-form/next")
    fun nextQuestionPaper(
        @RequestParam paperName: String,
        @RequestParam(required = false) selectedQuestionIds: Set<UUID>?,
        @SessionAttribute(QUESTION_PAPER_FORM_SESSION) questionPaperFormState: QuestionPaperFormState,
        model: Model,
    ): String {
        val createQuestionPaperRequest =
            CreateQuestionPaperRequest(name = paperName, questions = selectedQuestionIds.orEmpty())
        val questionPaperId = questionPaperService.saveAsDraft(createQuestionPaperRequest).id

        updateQuestionPaperFormState(questionPaperFormState, paperName, selectedQuestionIds, questionPaperId)
        return renderQuestionPaperNextForm(model, questionPaperFormState)
    }

    @PostMapping("/question-paper-form/review")
    fun reviewQuestionPaper(
        @RequestParam tagsJson: String,
        @SessionAttribute(QUESTION_PAPER_FORM_SESSION) questionPaperFormState: QuestionPaperFormState,
        model: Model,
    ): String {
        updateQuestionPaperTags(questionPaperFormState, tagsJson)
        return renderQuestionPaperReview(model, questionPaperFormState)
    }

    @PostMapping("/question-paper-form/submit")
    fun submitQuestionPaper(
        @SessionAttribute(QUESTION_PAPER_FORM_SESSION) questionPaperFormState: QuestionPaperFormState,
        redirectAttributes: RedirectAttributes,
        sessionStatus: SessionStatus
    ): String {
        val questionPaperId = requireNotNull(questionPaperFormState.questionPaperId) {
            "Question paper draft is missing."
        }
        questionPaperService.saveAsFinal(questionPaperId)

        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Question paper ${questionPaperFormState.paperName} submitted.",
        )
        sessionStatus.setComplete()

        return "redirect:/question-papers/$questionPaperId"
    }

    @GetMapping("/question-paper-form/questions")
    @ResponseBody
    fun getQuestionsByTags(
        @RequestParam tagIds: Set<UUID>,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): QuestionSearchPage {
        return questionService.getQuestions(tagIds, page, size)
    }

    private fun updateQuestionPaperFormState(
        questionPaperFormState: QuestionPaperFormState,
        paperName: String,
        selectedQuestionIds: Set<UUID>?,
        questionPaperId: UUID? = null,
    ) {
        questionPaperFormState.questionPaperId = questionPaperId
        questionPaperFormState.paperName = paperName.trim()
        questionPaperFormState.selectedQuestionIds.clear()
        questionPaperFormState.selectedQuestionIds.addAll(selectedQuestionIds.orEmpty())
    }

    private fun updateQuestionPaperTags(
        questionPaperFormState: QuestionPaperFormState,
        tagsJson: String,
    ) {
        questionPaperService.saveTags(questionPaperFormState.questionPaperId!!, tagsJson)
        questionPaperFormState.tagsJson = tagsJson.trim()
    }

    private fun renderQuestionPaperForm(
        model: Model,
        questionPaperFormState: QuestionPaperFormState,
    ): String {
        model.addAttribute(
            "questionPaperForm",
            QuestionPaperForm(
                paperName = questionPaperFormState.paperName,
                tagsJson = questionPaperFormState.tagsJson,
            ),
        )
        model.addAttribute("availableTagsJson", objectMapper.writeValueAsString(questionService.getAvailableTags()))
        return "question-paper-form"
    }

    private fun renderQuestionPaperNextForm(
        model: Model,
        questionPaperFormState: QuestionPaperFormState,
    ): String {
        model.addAttribute("questionPaperFormState", questionPaperFormState)
        model.addAttribute(
            "questionPaperForm",
            QuestionPaperForm(
                paperName = questionPaperFormState.paperName,
                tagsJson = questionPaperFormState.tagsJson,
            ),
        )
        model.addAttribute(
            "availableTagsJson",
            objectMapper.writeValueAsString(questionPaperService.getAvailableTags())
        )
        return "question-paper-form-next"
    }

    private fun renderQuestionPaperReview(
        model: Model,
        questionPaperFormState: QuestionPaperFormState,
    ): String {
        val questionPaperReview = questionPaperService.getQuestionPaper(questionPaperFormState.questionPaperId!!)

        model.addAttribute("questionPaperFormState", questionPaperFormState)
        model.addAttribute("questionPaperTags", tagHelper.toKeyValues(questionPaperFormState.tagsJson))

        model.addAttribute("selectedQuestions", questionPaperFormState.selectedQuestionIds)
        model.addAttribute("selectedQuestionDetails", questionPaperReview.questions)
        model.addAttribute("selectedQuestionTags", questionPaperReview.uniqueTags)
        return "question-paper-review"
    }
}
