package com.au.swarin.selective_tests.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
//                    .requestMatchers("/questions", "/tests", "/tests/**", "/error").permitAll()
                    .anyRequest().permitAll()
            }
            .csrf { csrf -> csrf.disable() }
            .httpBasic(Customizer.withDefaults())

        return http.build()
    }
}
