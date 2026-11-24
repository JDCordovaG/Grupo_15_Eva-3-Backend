package com.BuildMyPC.internal_security;

import feign.RequestInterceptor;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InternalServiceSecurityAutoConfiguration {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    @Bean
    SecurityFilterChain internalServiceSecurityFilterChain(
            HttpSecurity http,
            @Value("${internal.service.token:local-development-only}") String internalToken) throws Exception {
        byte[] expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);

        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**").permitAll()
                        .requestMatchers("/api/**").access((authentication, context) -> {
                            String provided = context.getRequest().getHeader(INTERNAL_TOKEN_HEADER);
                            byte[] providedToken = provided == null
                                    ? new byte[0]
                                    : provided.getBytes(StandardCharsets.UTF_8);
                            return new org.springframework.security.authorization.AuthorizationDecision(
                                    MessageDigest.isEqual(expectedToken, providedToken));
                        })
                        .anyRequest().permitAll())
                .build();
    }

    @Bean
    RequestInterceptor internalServiceTokenInterceptor(
            @Value("${internal.service.token:local-development-only}") String internalToken) {
        return template -> template.header(INTERNAL_TOKEN_HEADER, internalToken);
    }
}
