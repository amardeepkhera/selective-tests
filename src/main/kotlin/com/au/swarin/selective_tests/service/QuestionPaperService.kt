package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.repository.Paper
import com.au.swarin.selective_tests.repository.PaperQuestion
import com.au.swarin.selective_tests.repository.QuestionPaper
import com.au.swarin.selective_tests.repository.QuestionPaperRepository
import com.au.swarin.selective_tests.repository.QuestionRepository
import com.au.swarin.selective_tests.repository.TagRepository
import com.au.swarin.selective_tests.web.model.QuestionPaperListItem
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

private const val QUESTION_PAPER_TAG_ENTITY = "question_paper"

@Service
class QuestionPaperService(
    private val questionPaperRepository: QuestionPaperRepository,
    private val questionRepository: QuestionRepository,
    private val tagRepository: TagRepository,
    private val objectMapper: ObjectMapper,
) {
    fun getAllQuestionPapers(): List<QuestionPaperListItem> =
        questionPaperRepository.findAll().map { questionPaper ->
            QuestionPaperListItem(
                id = questionPaper.id,
                paper = questionPaper.paper,
                tags = questionPaper.tags,
                createdAt = questionPaper.createdAt,
            )
        }

    fun getAvailableTags(): List<Tag> = tagRepository.findAllByEntity(QUESTION_PAPER_TAG_ENTITY)
        .map { Tag(id = it.id!!, key = it.key, value = it.value) }

    @Transactional
    fun saveAsDraft(createQuestionPaperRequest: CreateQuestionPaperRequest) {
        val paper = with(createQuestionPaperRequest) {
            Paper(
                name = name,
                questions = questions.map { PaperQuestion(questionId = it) }
            )
        }
        val questionPaper = QuestionPaper(paper = paper, status = "DRAFT", createdAt = LocalDateTime.now(UTC))
        questionPaperRepository.save(questionPaper)
    }
}
