package com.au.swarin.selective_tests.config

import com.au.swarin.selective_tests.repository.Images
import com.au.swarin.selective_tests.repository.Options
import com.au.swarin.selective_tests.repository.Paper
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.postgresql.util.PGobject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions

@Configuration
class JdbcJsonConvertersConfig {

    @Bean
    fun jdbcCustomConversions(objectMapper: ObjectMapper): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(
                PgObjectToJsonNodeConverter(objectMapper),
                JsonNodeToPgObjectConverter(objectMapper),
                PgObjectToPaperConverter(objectMapper),
                PaperToPgObjectConverter(objectMapper),
                PgObjectToImagesConverter(objectMapper),
                ImagesToPgObjectConverter(objectMapper),
                PgObjectToOptionsConverter(objectMapper),
                OptionsToPgObjectConverter(objectMapper),
            ),
        )
    }
}


@ReadingConverter
class PgObjectToJsonNodeConverter(
    private val objectMapper: ObjectMapper,
) : Converter<PGobject, JsonNode> {
    override fun convert(source: PGobject): JsonNode = objectMapper.readTree(source.value ?: "null")
}

@WritingConverter
class JsonNodeToPgObjectConverter(
    private val objectMapper: ObjectMapper,
) : Converter<JsonNode, PGobject> {
    override fun convert(source: JsonNode): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = objectMapper.writeValueAsString(source)
        }
}

@ReadingConverter
class PgObjectToPaperConverter(
    private val objectMapper: ObjectMapper,
) : Converter<PGobject, Paper> {
    override fun convert(source: PGobject): Paper = objectMapper.readValue(source.value ?: "{}")
}

@WritingConverter
class PaperToPgObjectConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Paper, PGobject> {
    override fun convert(source: Paper): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = objectMapper.writeValueAsString(source)
        }
}

@ReadingConverter
class PgObjectToImagesConverter(
    private val objectMapper: ObjectMapper,
) : Converter<PGobject, Images> {
    override fun convert(source: PGobject): Images = fromJson<Images>(source, objectMapper)
}

@WritingConverter
class ImagesToPgObjectConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Images, PGobject> {
    override fun convert(source: Images): PGobject = PGobject().toJson(source, objectMapper)
}

@ReadingConverter
class PgObjectToOptionsConverter(
    private val objectMapper: ObjectMapper,
) : Converter<PGobject, Options> {
    override fun convert(source: PGobject): Options = fromJson<Options>(source, objectMapper)
}

@WritingConverter
class OptionsToPgObjectConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Options, PGobject> {
    override fun convert(source: Options): PGobject = PGobject().toJson(source, objectMapper)
}

private inline fun <reified T> fromJson(source: PGobject, objectMapper: ObjectMapper): T =
    objectMapper.readValue(source.value ?: "{}")

private fun PGobject.toJson(source: Any, objectMapper: ObjectMapper) = apply {
    type = "jsonb"
    value = objectMapper.writeValueAsString(source)
}
