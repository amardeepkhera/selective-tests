package com.au.swarin.selective_tests.web

import com.au.swarin.selective_tests.service.QuestionCreationRequest
import com.au.swarin.selective_tests.service.QuestionService
import com.au.swarin.selective_tests.web.model.QuestionForm
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class QuestionController(
    private val questionService: QuestionService,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping("/questions")
    fun questions(model: Model): String {
        model.addAttribute("questions", questionService.getAllQuestions())
        return "questions"
    }

    @GetMapping("/questions/new")
    fun newQuestion(model: Model): String {
        if (!model.containsAttribute("questionForm")) {
            model.addAttribute("questionForm", QuestionForm())
        }
        model.addAttribute("availableTagsJson", objectMapper.writeValueAsString(questionService.getAvailableTags()))
        return "question-form"
    }

    @PostMapping("/questions")
    fun createQuestion(
        @ModelAttribute questionForm: QuestionForm,
        redirectAttributes: RedirectAttributes,
    ): String {
        return try {
            questionService.createQuestion(
                QuestionCreationRequest(
                    text = questionForm.text,
                    imagesJson = questionForm.imagesJson,
                    optionsJson = questionForm.optionsJson,
                    tagsJson = questionForm.tagsJson,
                    answer = questionForm.answer,
                ),
            )
            redirectAttributes.addFlashAttribute("successMessage", "Question created successfully.")
            "redirect:/questions"
        } catch (ex: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.message ?: "Unable to create question.")
            redirectAttributes.addFlashAttribute("questionForm", questionForm)
            "redirect:/questions/new"
        }
    }
}
