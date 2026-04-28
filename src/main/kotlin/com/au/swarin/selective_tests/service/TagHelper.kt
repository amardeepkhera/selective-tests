package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.nowAtUTCAndTruncatedToMins
import com.au.swarin.selective_tests.repository.Tag
import com.au.swarin.selective_tests.repository.TagRepository
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class TagHelper(
    private val objectMapper: ObjectMapper,
    private val tagRepository: TagRepository,
) {

    fun saveQuestionPaperTags(tagsJson: String): Set<UUID>? {
        return saveTags(tagsJson, "question_paper")
    }

    fun saveQuestionTags(tagsJson: String): Set<UUID>? {
        return saveTags(tagsJson, "question")
    }

    fun toKeyValues(tagsJson: String?): Set<String> = if (tagsJson.isNullOrBlank()) emptySet() else
        objectMapper.readTree(tagsJson).map { "${it.key()}:${it.value()}" }.toSet()


    private fun saveTags(tagsJson: String, entity: String): Set<UUID>? {
        if (tagsJson.isBlank()) {
            return null
        }
        val tags = objectMapper.readTree(tagsJson.trim())
        val existingTags = mutableSetOf<UUID>()
        val newTags = mutableSetOf<Tag>()
        val now = nowAtUTCAndTruncatedToMins()
        tags.forEach { entry ->
            when {
                entry.isString -> existingTags += UUID.fromString(entry.asString())
                entry.isObject -> {
                    val existingTagId = entry.id()
                    if (existingTagId != null) {
                        existingTags += existingTagId
                    } else {
                        newTags += Tag(
                            entity = entity,
                            key = entry.key(),
                            value = entry.value(),
                            createdAt = now
                        )
                    }
                }

                else -> throw IllegalArgumentException("Tags JSON contains an invalid tag entry.")
            }
        }
        return tagRepository.saveAll(newTags)!!.map { it.id!! }.toSet() + existingTags
    }

    private fun JsonNode.id(): UUID? = path("id").asString("")
        .trim()
        .takeIf { it.isNotBlank() }
        ?.let(UUID::fromString)

    private fun JsonNode.key(): String = path("key").asString("").trim()
    private fun JsonNode.value(): String = path("value").asString("").trim()
}
