package com.au.swarin.selective_tests.repository

import org.springframework.data.repository.ListCrudRepository
import java.util.UUID

interface TestRepository : ListCrudRepository<Test, UUID>
