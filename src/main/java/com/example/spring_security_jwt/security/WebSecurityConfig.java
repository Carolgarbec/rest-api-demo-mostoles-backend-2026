package com.example.spring_security_jwt.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.spring_security_jwt.security.jwt.AuthEntryPointJwt;
import com.example.spring_security_jwt.security.jwt.AuthTokenFilter;
import com.example.spring_security_jwt.security.jwt.JwtUtils;
import com.example.spring_security_jwt.security.service.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
/**
 * La anotacion anterior permite securedEnable = true, 
 * jsr250Enabled = true y la mas importante prePostEnabled = true
 * 
 * Que se resume a poder asegurar (securizar) directamente los metodos
 * de los controladores, es decir, donde se delegan las peticiones,
 * concretamente los endpoints
 * 
 */
@RequiredArgsConstructor
public class WebSecurityConfig {
	
	private final UserDetailsServiceImpl userDetailsService;
	private final AuthEntryPointJwt unauthorizeHandle;
	private final JwtUtils jwtUtils;
	
	@Bean
	AuthTokenFilter authenticationJwtTokenFilter() {
		
		return new AuthTokenFilter(jwtUtils, userDetailsService);
	}
	
	@SuppressWarnings("null")
	@Bean
	DaoAuthenticationProvider authenticationProvider() {

		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
		
		authProvider.setPasswordEncoder(passwordEncoder());
		
		return authProvider;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {
		return authConfig.getAuthenticationManager();
	}
	
	// El bean siguiente es el que hay que saber personalizar para adaptarlo a nuestro proyecto
	// todo lo demas es boilerplate (codigo repetitivo)
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) {
		
		http.csrf(csrf -> csrf.disable())
			.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizeHandle))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**").permitAll()
					.anyRequest().authenticated());
		
		http.authenticationProvider(authenticationProvider());
		
		http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
}









