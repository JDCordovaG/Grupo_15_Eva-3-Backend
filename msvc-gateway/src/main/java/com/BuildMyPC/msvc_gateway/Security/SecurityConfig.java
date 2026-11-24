package com.BuildMyPC.msvc_gateway.Security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenService jwt) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwt);
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                escribirError(response, HttpServletResponse.SC_UNAUTHORIZED, "Sesión no válida o vencida"))
                        .accessDeniedHandler((request, response, exception) ->
                                escribirError(response, HttpServletResponse.SC_FORBIDDEN, "Sin permiso para esta acción")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/docs/swagger-ui.html",
                                "/docs/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auths", "/api/v1/auths/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/components/**", "/api/v1/cpus/**", "/api/v1/gpus/**",
                                "/api/v1/placas-madre/**", "/api/v1/rams/**", "/api/v1/fuentes-poder/**",
                                "/api/v1/benchmarks/**", "/api/v1/compatibilities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/builds/pendientes-revision",
                                "/api/v1/builds/revisadas").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/builds").hasAnyRole("ADMINISTRADOR", "TECNICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/builds/*/observaciones-tecnicas").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/builds/*/observaciones-tecnicas").hasRole("TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/auths/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/auths/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/components/**", "/api/v1/cpus/**",
                                "/api/v1/gpus/**", "/api/v1/placas-madre/**", "/api/v1/rams/**",
                                "/api/v1/fuentes-poder/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/components/**", "/api/v1/cpus/**",
                                "/api/v1/gpus/**", "/api/v1/placas-madre/**", "/api/v1/rams/**",
                                "/api/v1/fuentes-poder/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/components/**", "/api/v1/cpus/**",
                                "/api/v1/gpus/**", "/api/v1/placas-madre/**", "/api/v1/rams/**",
                                "/api/v1/fuentes-poder/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/components/**", "/api/v1/cpus/**",
                                "/api/v1/gpus/**", "/api/v1/placas-madre/**", "/api/v1/rams/**",
                                "/api/v1/fuentes-poder/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void escribirError(HttpServletResponse response, int status, String mensaje) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
