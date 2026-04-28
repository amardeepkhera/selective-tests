package com.au.swarin.selective_tests.config

import org.springframework.context.annotation.Configuration
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession

@Configuration
@EnableJdbcHttpSession
class SessionConfig
