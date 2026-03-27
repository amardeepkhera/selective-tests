package com.au.swarin.selective_tests.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper =
        JsonMapper.builder()
            .defaultPropertyInclusion(
                JsonInclude.Value.construct(
                    JsonInclude.Include.NON_NULL,
                    JsonInclude.Include.NON_NULL,
                ),
            )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .build()
            .registerModule(KotlinModule.Builder().build())
            .findAndRegisterModules()
}
