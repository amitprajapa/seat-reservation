package com.amit.seatreservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable())

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**").permitAll()
						
						
						// Frontend pages
					    .requestMatchers(
					        "/",
					        "/index.html",
					        "/login.html",
					        "/admin-register.html",
					        "/seat-selection.html",
					        "/bookings.html",
					        "/register.html",
					        "/admin.html"
					    ).permitAll()

					    // CSS, JavaScript, images and other static files
					    .requestMatchers(
					        "/css/**",
					        "/js/**",
					        "/images/**",
					        "/favicon.ico"
					    ).permitAll()

//					    .requestMatchers(
//					        "/api/auth/register",
//					        "/api/auth/login"
//					    ).permitAll()

					    .requestMatchers(
					        "/actuator/health",
					        "/actuator/health/**"
					    ).permitAll()

					    // Admin-only operations
					    .requestMatchers("/api/admin/**")
					    .hasRole("ADMIN")

					    // Customer and admin can view shows
					    .requestMatchers(
					    	    HttpMethod.GET,
					    	    "/api/shows",
					    	    "/api/shows/**"
					    	).permitAll()
					    
					    .requestMatchers(
					            "/actuator/health",
					            "/actuator/health/**",
					            "/actuator/info",
					            "/actuator/metrics",
					            "/actuator/metrics/**",
					            "/actuator/prometheus"
					    ).permitAll()
					    
					    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
					    .requestMatchers("/api/auth/logout").authenticated()

					    // All other endpoints require authentication
					    .anyRequest().authenticated()
					)

				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {

		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

		authoritiesConverter.setAuthorityPrefix("ROLE_");
		authoritiesConverter.setAuthoritiesClaimName("roles");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

		return converter;
	}
}