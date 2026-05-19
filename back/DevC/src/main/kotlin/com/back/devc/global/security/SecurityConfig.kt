package com.back.devc.global.security

import com.back.devc.global.security.jwt.JwtAuthenticationFilter
import com.back.devc.global.security.oauth2.OAuth2LoginFailureHandler
import com.back.devc.global.security.oauth2.OAuth2LoginSuccessHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    @Order(1)
    @Throws(Exception::class)
    fun oauth2FilterChain(
        http: HttpSecurity,
        clientRegistrationRepositoryProvider: ObjectProvider<ClientRegistrationRepository>,
        oauth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
        oauth2LoginFailureHandler: OAuth2LoginFailureHandler
    ): SecurityFilterChain {
        http
            .securityMatcher("/oauth2/**", "/login/oauth2/**")
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().permitAll()
            }
            .cors(Customizer.withDefaults())
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }

        if (clientRegistrationRepositoryProvider.ifAvailable != null) {
            http.oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureHandler(oauth2LoginFailureHandler)
            }
        }

        return http.build()
    }

    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun apiFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        customAuthenticationEntryPoint: CustomAuthenticationEntryPoint
    ): SecurityFilterChain {
        http
            .securityMatcher("/api/**", "/h2-console/**", "/error", "/favicon.ico")
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/favicon.ico").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/users/me").authenticated()
                    .requestMatchers("/api/users/me/likes").authenticated()
                    .requestMatchers("/api/mypage/**").authenticated()
                    .requestMatchers("/api/report/**").authenticated()
                    .requestMatchers("/api/posts/*/likes").authenticated()
                    .requestMatchers("/api/posts/*/bookmarks").authenticated()
                    .anyRequest().permitAll()
            }
            .cors(Customizer.withDefaults())
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(customAuthenticationEntryPoint)
            }
            .headers { headers ->
                headers.addHeaderWriter(
                    XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)
                )
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    @Order(3)
    @Throws(Exception::class)
    fun defaultFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().permitAll()
            }
            .cors(Customizer.withDefaults())
            .csrf { csrf -> csrf.disable() }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler { _, response, _ ->
                        response.status = 200
                    }
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
            }

        return http.build()
    }
}
