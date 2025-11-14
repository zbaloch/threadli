package com.threadli.threadli_web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.threadli.threadli_web.services.CustomUserDetailsService;

import javax.sql.DataSource;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    @Autowired
	private DataSource dataSource;

    @Bean
	public UserDetailsService userDetailsService() {
		return new CustomUserDetailsService();
	}

    @Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

    @Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService());
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

    @Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests((requests) -> requests
				.requestMatchers("/", "/home", "/signup", "/forgot-password", 
				"/w/create-team",
				"/threadli/**",
				"/create-user", "/login", "/favicon.ico", "/assets/**",
				"/api/webhook",
				"/create-user-magic", "/login-magic", "create-user-or-login-magic",
				"/verify-token-and-login",
				"/privacy", "/terms",
				"/scrapper/stat-cr-scrapping",
				"/tools/**",
				"/tools"
				).permitAll()
				.anyRequest().authenticated()
			)
			.formLogin((form) -> form
				.loginPage("/login")
				.usernameParameter("email")
				.passwordParameter("password")
				.defaultSuccessUrl("/dashboard", false)
				.permitAll()
			)
			.logout((logout) -> logout.permitAll())
			.csrf().disable()
			// .rememberMe((rememberMe) -> rememberMe.key("victor-detail-oriented"))
			// .securityContextRepository(securityContextRepository())
			// .securityContext(
			// 	(securityContext) -> securityContext.securityContextRepository(new RequestAttributeSecurityContextRepository())
			// )
			;

		return http.build();
	} 

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(
			new RequestAttributeSecurityContextRepository(),
			new HttpSessionSecurityContextRepository()
		);
	}

    
}
