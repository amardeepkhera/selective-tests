package com.au.swarin.selective_tests

import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit

fun nowAtUTCAndTruncatedToMins(): LocalDateTime = now(UTC).truncatedTo(
    ChronoUnit.MINUTES
)