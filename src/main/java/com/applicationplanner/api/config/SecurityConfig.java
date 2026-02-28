package com.applicationplanner.api.config;

import com.applicationplanner.api.auth.JwtService;
import com.applicationplanner.api.error.ApiError;
import com.applicationplanner.api.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, ObjectMapper objectMapper) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.headers(h -> h.frameOptions(f -> f.disable()));

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.httpBasic(b -> b.disable());
        http.formLogin(f -> f.disable());
        http.logout(l -> l.disable());

        http.exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setCharacterEncoding("UTF-8");
                    res.setContentType("application/json");
                    res.getWriter().write(
                            objectMapper.writeValueAsString(
                                    ApiError.simple(401, "UNAUTHORIZED", "Unauthorized", req.getRequestURI())
                            )
                    );
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setCharacterEncoding("UTF-8");
                    res.setContentType("application/json");
                    res.getWriter().write(
                            objectMapper.writeValueAsString(
                                    ApiError.simple(403, "FORBIDDEN", "Forbidden", req.getRequestURI())
                            )
                    );
                })
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
        );

        http.addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}