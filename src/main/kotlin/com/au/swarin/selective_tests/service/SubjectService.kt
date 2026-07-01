package com.au.swarin.selective_tests.service

import com.au.swarin.selective_tests.repository.Subject
import com.au.swarin.selective_tests.repository.SubjectRepository
import org.springframework.stereotype.Service

@Service
class SubjectService(private val subjectRepository: SubjectRepository) {
    fun getAllSubjects(): List<Subject> = subjectRepository.findAll().sortedBy { it.name.lowercase() }
}