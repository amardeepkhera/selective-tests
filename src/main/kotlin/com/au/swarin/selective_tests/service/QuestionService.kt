package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.repository.Images
import com.au.swarin.selective_tests.repository.Options
import com.au.swarin.selective_tests.repository.Question
import com.au.swarin.selective_tests.repository.QuestionRepository
import com.au.swarin.selective_tests.repository.TagRepository
import com.au.swarin.selective_tests.web.model.QuestionListItem
import com.au.swarin.selective_tests.web.model.QuestionSearchPage
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val tagRepository: TagRepository,
    private val objectMapper: ObjectMapper,
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
        val visibleQuestions = questions
            .take(safeSize)
            .map { question ->
                QuestionListItem(
                    id = question.id,
                    text = question.text,
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

    @Transactional
    fun createQuestion(request: QuestionCreationRequest): Question {
        val text = request.text.trim()
        val answer = request.answer.trim()
        require(text.isNotBlank()) { "Question text is required." }
        require(answer.isNotBlank()) { "Answer is required." }

        val images = parseImages(request.imagesJson)
        val options = parseOptions(request.optionsJson)
        val tags = parseTags(request.tagsJson)

        require(options.options.isNotEmpty()) { "At least one option is required." }
        require(options.options.any { it.label == answer }) { "Answer must match one of the option labels." }

        return questionRepository.save(
            Question(
                text = text,
                images = images,
                options = options,
                answer = answer,
                tags = tags,
                createdAt = LocalDateTime.now(UTC),
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

    private fun parseTags(tagsJson: String): JsonNode? {
        val trimmed = tagsJson.trim()
        if (trimmed.isBlank()) {
            return null
        }

        return try {
            val parsed = objectMapper.readTree(trimmed)
            require(parsed.isArray) { "Tags JSON must be an array." }
            resolveTags(parsed)
        } catch (_: JsonProcessingException) {
            throw IllegalArgumentException("Tags JSON is invalid.")
        }
    }

    private fun resolveTags(parsed: JsonNode): JsonNode? {
        val resolvedTags = objectMapper.createArrayNode()

        parsed.forEach { entry ->
            when {
                entry.isTextual -> resolvedTags.add(resolveExistingTag(entry.asText()).toString())
                entry.isObject -> resolvedTags.add(resolveSubmittedTag(entry).toString())
                else -> throw IllegalArgumentException("Tags JSON contains an invalid tag entry.")
            }
        }

        return resolvedTags.takeIf { it.size() > 0 }
    }

    private fun resolveExistingTag(tagId: String): UUID {
        val parsedId = try {
            UUID.fromString(tagId)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Tags JSON contains an invalid tag id.")
        }

        val tag = tagRepository.findById(parsedId).orElseThrow {
            IllegalArgumentException("Tag does not exist.")
        }
        require(tag.entity == "question") { "Tag does not belong to questions." }
        return parsedId
    }

    private fun resolveSubmittedTag(entry: JsonNode): UUID {
        val key = entry.path("key").asText("").trim()
        val value = entry.path("value").asText("").trim()
        require(key.isNotBlank() && value.isNotBlank()) { "New tags must include key and value." }

        return try {
            tagRepository.save(
                com.au.swarin.selective_tests.repository.Tag(
                    entity = "question",
                    key = key,
                    value = value,
                    createdAt = LocalDateTime.now(UTC),
                ),
            ).id ?: throw IllegalStateException("Saved tag id is missing.")
        } catch (_: DataIntegrityViolationException) {
            tagRepository.findByEntityAndKeyAndValue("question", key, value)?.id
                ?: throw IllegalStateException("Existing tag was not found after save conflict.")
        }
    }
}
