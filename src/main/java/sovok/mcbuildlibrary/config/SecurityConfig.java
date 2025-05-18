package sovok.mcbuildlibrary.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import sovok.mcbuildlibrary.model.Role;
import sovok.mcbuildlibrary.service.UserDetailsServiceImpl;

/**
 * Configures web security for the application.
 * Enables method-level security and defines HTTP security rules,
 * including custom success and logout handlers for SPA compatibility.
 * CSRF protection is enabled using CookieCsrfTokenRepository.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final UserDetailsServiceImpl userDetailsService;
    private final RestAuthenticationSuccessHandler restAuthenticationSuccessHandler;
    
    private static final String API_AUTHORS_PATH = "/api/authors/**";
    private static final String API_THEMES_PATH = "/api/themes/**";
    private static final String API_COLORS_PATH = "/api/colors/**";
    private static final String API_BUILDS_PATH = "/api/builds";
    private static final String API_BUILDS_ID_PATH = "/api/builds/{identifier}/**";
    private static final String ROLE_USER_STRING = Role.ROLE_USER.name().replace("ROLE_", "");
    private static final String ROLE_ADMIN_STRING = Role.ROLE_ADMIN.name().replace("ROLE_", "");
    
    
    private static final String LOGIN_PROCESSING_URL = "/api/perform_login";
    private static final String LOGOUT_PROCESSING_URL = "/api/perform_logout";
    
    /**
     * Constructs the SecurityConfig.
     *
     * @param userDetailsService             Service to load user-specific data.
     * @param restAuthenticationSuccessHandler Custom handler for successful authentication.
     */
    @Autowired
    public SecurityConfig(
            UserDetailsServiceImpl userDetailsService,
            @Qualifier("restAuthenticationSuccessHandler")
            RestAuthenticationSuccessHandler restAuthenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.restAuthenticationSuccessHandler = restAuthenticationSuccessHandler;
    }
    
    /**
     * Provides a PasswordEncoder bean for hashing passwords.
     *
     * @return A {@link BCryptPasswordEncoder} instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Provides an AuthenticationManager bean.
     *
     * @param authenticationConfiguration The authentication configuration.
     * @return An {@link AuthenticationManager} instance.
     * @throws Exception if an error occurs while getting the authentication manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    /**
     * Configures the security filter chain for HTTP requests.
     * Defines authorization rules for various API endpoints and SPA routes.
     * CSRF protection is enabled using CookieCsrfTokenRepository.
     *
     * @param http The {@link HttpSecurity} to configure.
     * @return The configured {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);
        
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/", "/index.html",
                                "/static/**", "/assets/**",
                                "/css/**", "/js/**", "/images/**",
                                "/vite.svg", "/manifest.json", "/favicon.ico",
                                "/login", "/register",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/actuator/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/register")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, API_BUILDS_PATH,
                                "/api/builds/{identifier}",
                                "/api/builds/{identifier}/schem")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, API_AUTHORS_PATH, API_THEMES_PATH,
                                API_COLORS_PATH)
                        .permitAll()
                        .requestMatchers("/api/users/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, API_BUILDS_PATH)
                        .hasRole(ROLE_USER_STRING)
                        .requestMatchers(HttpMethod.PUT, API_BUILDS_ID_PATH)
                        .hasRole(ROLE_ADMIN_STRING)
                        .requestMatchers(HttpMethod.DELETE, API_BUILDS_ID_PATH)
                        .hasRole(ROLE_ADMIN_STRING)
                        .requestMatchers(HttpMethod.POST, API_AUTHORS_PATH, API_THEMES_PATH,
                                API_COLORS_PATH)
                        .hasRole(ROLE_ADMIN_STRING)
                        .requestMatchers(HttpMethod.PUT, API_AUTHORS_PATH, API_THEMES_PATH,
                                API_COLORS_PATH)
                        .hasRole(ROLE_ADMIN_STRING)
                        .requestMatchers(HttpMethod.DELETE, API_AUTHORS_PATH, API_THEMES_PATH,
                                API_COLORS_PATH)
                        .hasRole(ROLE_ADMIN_STRING)
                        .requestMatchers("/api/bulk/**")
                        .hasRole(ROLE_ADMIN_STRING)
                        .requestMatchers(HttpMethod.POST, "/api/builds/{identifier}/generate-log")
                        .hasAnyRole(ROLE_USER_STRING, ROLE_ADMIN_STRING)
                        .requestMatchers(HttpMethod.GET, "/api/builds/log-status/{taskId}",
                                "/api/builds/log-file/{taskId}")
                        .hasAnyRole(ROLE_USER_STRING, ROLE_ADMIN_STRING)
                        .requestMatchers("/logs/**", "/total-request-count")
                        .permitAll()
                        .anyRequest().authenticated())
                .userDetailsService(userDetailsService)
                .formLogin(formLogin ->
                                formLogin
                                        .loginProcessingUrl(LOGIN_PROCESSING_URL)
                                        .permitAll()
                                        .successHandler(restAuthenticationSuccessHandler)
                        
                )
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_PROCESSING_URL)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)
                        )
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll());
        
        return http.build();
    }
}