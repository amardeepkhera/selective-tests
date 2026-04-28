package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.nowAtUTCAndTruncatedToMins
import com.au.swarin.selective_tests.repository.Images
import com.au.swarin.selective_tests.repository.Options
import com.au.swarin.selective_tests.repository.Question
import com.au.swarin.selective_tests.repository.QuestionRepository
import com.au.swarin.selective_tests.repository.TagRepository
import com.au.swarin.selective_tests.web.model.QuestionListItem
import com.au.swarin.selective_tests.web.model.QuestionPaperReview
import com.au.swarin.selective_tests.web.model.QuestionSearchPage
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val tagRepository: TagRepository,
    private val objectMapper: ObjectMapper,
    private val tagHelper: TagHelper,
) {
    fun getAllQuestions(): List<QuestionListItem> {

        return questionRepository.findAll().map { question ->
            QuestionListItem(
                id = question.id,
                text = question.text,
                images = question.images,
                options = question.options,
                answer = question.answer,
                createdAt = question.createdAt,
            )
        }
    }

    fun getQuestionsByIds(questionIds: Set<UUID>): List<QuestionListItem> {
        if (questionIds.isEmpty()) {
            return emptyList()
        }

        val requestedIds = questionIds.toSet()
        val questionsById = questionRepository.findAllById(requestedIds)
            .associateBy { it.id }

        val uniqueTagIds = questionsById.values
            .mapNotNull { it.tags }
            .flatMap { objectMapper.treeToValue<Set<UUID>>(it) }

        val tagsMap = tagRepository.findAllById(uniqueTagIds)
            .associateBy { it.id!! }


        return questionIds.mapNotNull { questionId ->
            questionsById[questionId]?.let { question ->
                QuestionListItem(
                    id = question.id,
                    text = question.text,
                    images = question.images,
                    options = question.options,
                    answer = question.answer,
                    tags = question.tags?.let {
                        objectMapper.treeToValue<Set<UUID>>(it)
                            .map { tagsMap.getValue(it) }
                            .toSet()
                    } ?: emptySet(),
                    createdAt = question.createdAt,
                )
            }
        }
    }

    fun getQuestions(tags: Set<UUID>, page: Int, size: Int): QuestionSearchPage {
        require(page >= 0) { "Page index must be zero or greater." }
        require(size > 0) { "Page size must be greater than zero." }

        val safeSize = size.coerceAtMost(100)
        val offset = page.toLong() * safeSize
        val questions = questionRepository.findAllByAnyTagIdIn(
            tagIds = tags,
            limit = safeSize + 1,
            offset = offset,
        )

        val hasMore = questions.size > safeSize
        val uniqueTagIds = mutableSetOf<UUID>()

        val questionIdToTagIdsMap = questions
            .take(safeSize)
            .filter { it.tags != null }
            .associate {
                uniqueTagIds.addAll(objectMapper.treeToValue<Set<UUID>>(it.tags!!))
                it.id to objectMapper.treeToValue<Set<UUID>>(it.tags)
            }

        val tagsMap = tagRepository.findAllById(uniqueTagIds)
            .associateBy { it.id!! }


        val visibleQuestions = questions
            .take(safeSize)
            .map { question ->
                QuestionListItem(
                    id = question.id,
                    text = question.text,
                    tags = question.tags?.let {
                        questionIdToTagIdsMap.getValue(question.id!!)
                            .map { tagsMap.getValue(it) }
                            .toSet()
                    } ?: emptySet()
                )
            }

        return QuestionSearchPage(
            questions = visibleQuestions,
            page = page,
            size = safeSize,
            hasMore = hasMore,
        )
    }

    fun getQuestion(id: UUID): Question = questionRepository.findById(id).orElseThrow()

    fun getAvailableTags(): List<Tag> = tagRepository.findAllByEntity("question")
        .map { Tag(id = it.id!!, key = it.key, value = it.value) }

    fun getAvailableTags(questionIds: Set<UUID>): Set<Tag> = questionRepository.getTags(questionIds)
        .run { tagRepository.findAllById(this) }.map { Tag(id = it.id!!, key = it.key, value = it.value) }
        .toSet()

    @Transactional
    fun createQuestion(request: QuestionCreationRequest): Question {
        val text = request.text.trim()
        val answer = request.answer.trim()
        require(text.isNotBlank()) { "Question text is required." }
        require(answer.isNotBlank()) { "Answer is required." }

        val images = parseImages(request.imagesJson)
        val options = parseOptions(request.optionsJson)
        val allTags = tagHelper.saveQuestionTags(request.tagsJson)

        return questionRepository.save(
            Question(
                text = text,
                images = images,
                options = options,
                answer = answer,
                tags = objectMapper.convertValue<JsonNode>(allTags),
                createdAt = nowAtUTCAndTruncatedToMins(),
            ),
        )
    }

    private fun parseImages(imagesJson: String): Images? {
        val trimmed = imagesJson.trim()
        if (trimmed.isBlank()) {
            return null
        }
        return try {
            objectMapper.readValue<Images>(trimmed)
        } catch (_: JsonProcessingException) {
            throw IllegalArgumentException("Images JSON is invalid.")
        }
    }

    private fun parseOptions(optionsJson: String): Options {
        val trimmed = optionsJson.trim()
        require(trimmed.isNotBlank()) { "Options JSON is required." }
        return try {
            objectMapper.readValue(trimmed)
        } catch (_: JsonProcessingException) {
            throw IllegalArgumentException("Options JSON is invalid.")
        }
    }
}
