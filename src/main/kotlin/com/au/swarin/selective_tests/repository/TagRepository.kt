package com.au.swarin.selective_tests.repository

import org.springframework.data.repository.ListCrudRepository
import java.util.UUID

interface TagRepository : ListCrudRepository<Tag, UUID> {
    fun findAllByEntity(entity: String): List<Tag>
    fun findByEntityAndKeyAndValue(entity: String, key: String, value: String): Tag?
}
