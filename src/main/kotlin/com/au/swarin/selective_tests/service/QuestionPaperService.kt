package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.nowAtUTCAndTruncatedToMins
import com.au.swarin.selective_tests.repository.Paper
import com.au.swarin.selective_tests.repository.PaperQuestion
import com.au.swarin.selective_tests.repository.QuestionPaper
import com.au.swarin.selective_tests.repository.QuestionPaperRepository
import com.au.swarin.selective_tests.repository.QuestionRepository
import com.au.swarin.selective_tests.repository.TagRepository
import com.au.swarin.selective_tests.web.model.DetailedQuestionPaper
import com.au.swarin.selective_tests.web.model.QuestionListItem
import com.au.swarin.selective_tests.web.model.QuestionPaperListItem
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
class QuestionPaperService(
    private val questionRepository: QuestionRepository,
    private val questionPaperRepository: QuestionPaperRepository,
    private val tagRepository: TagRepository,
    private val objectMapper: ObjectMapper,
    private val tagHelper: TagHelper
) {
    fun getAllQuestionPapers(): List<QuestionPaperListItem> =
        questionPaperRepository.findAll().map { questionPaper ->
            QuestionPaperListItem(
                id = questionPaper.id,
                paper = questionPaper.paper,
                status = questionPaper.status,
                tags = questionPaper.tags,
                createdAt = questionPaper.createdAt,
            )
        }

    fun getQuestionPaperById(questionPaperId: UUID): QuestionPaper? =
        questionPaperRepository.findById(questionPaperId).orElse(null)

    fun getAvailableTags(): List<Tag> = tagRepository.findAllByEntity("question_paper")
        .map { Tag(id = it.id!!, key = it.key, value = it.value) }

    @Transactional
    fun saveAsDraft(createQuestionPaperRequest: CreateQuestionPaperRequest): QuestionPaper {
        val paper = with(createQuestionPaperRequest) {
            Paper(
                name = name,
                questions = questions.mapIndexed { index, id -> PaperQuestion(questionNo = index, questionId = id) }
            )
        }
        val questionPaper =
            QuestionPaper(paper = paper, status = "DRAFT", createdAt = nowAtUTCAndTruncatedToMins())
        return questionPaperRepository.save(questionPaper)!!
    }

    @Transactional
    fun saveAsFinal(questionPaperId: UUID) {
        questionPaperRepository.updateStatus(questionPaperId, "FINAL")
    }

    @Transactional
    fun saveTags(questionPaperId: UUID, tagsJson: String) {
        val allTagIds = tagHelper.saveQuestionPaperTags(tagsJson)

        objectMapper.convertValue(allTagIds, JsonNode::class.java).run {
            questionPaperRepository.addTags(questionPaperId, this)
        }
    }

    fun getQuestionPaperWithoutTags(questionPaperId: UUID): DetailedQuestionPaper {
        val questionPaper = questionPaperRepository.findById(questionPaperId).orElseThrow()

        val questionIdToQuestion = questionPaper.paper.questions.associateBy { it.questionId }

        return questionRepository.findAllById(questionIdToQuestion.keys)
            .map { question ->
                QuestionListItem(
                    id = question.id,
                    questionNo = questionIdToQuestion[question.id]?.questionNo,
                    text = question.text,
                )
            }.sortedBy { it.questionNo }
            .run { DetailedQuestionPaper(
                name = questionPaper.paper.name,
                questions = this, uniqueTags = emptySet(), isFinal = questionPaper.isFinal()) }

    }

    fun getQuestionPaper(questionPaperId: UUID): DetailedQuestionPaper {
        val questionPaper = questionPaperRepository.findById(questionPaperId).orElseThrow()

        val questionIdToQuestion = questionPaper.paper.questions.associateBy { it.questionId }

        val questions = questionRepository.findAllById(questionIdToQuestion.keys)

        val questionIdToTag = questions.associate { it.id!! to objectMapper.convertValue<Set<UUID>>(it.tags) }

        val tagIdToTag = tagRepository.findAllById(questionIdToTag.values.toSet().flatten())
            .associateBy { it.id!! }

        val questionsList = questions
            .map { question ->
                QuestionListItem(
                    id = question.id,
                    questionNo = questionIdToQuestion[question.id]?.questionNo,
                    text = question.text,
                    tags = questionIdToTag[question.id]?.let { tags ->
                        tags.map { tagIdToTag.getValue(it) }.toSet()
                    } ?: emptySet(),
                    createdAt = question.createdAt,
                )

            }.sortedBy { it.questionNo }

        return DetailedQuestionPaper(
            name = questionPaper.paper.name,
            questions = questionsList,
            uniqueTags = tagIdToTag.values.map { "${it.key}:${it.value}" }.toSet(),
            isFinal = questionPaper.isFinal()
        )
    }
}
