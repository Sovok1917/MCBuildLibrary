// file: src/main/java/sovok/mcbuildlibrary/config/SecurityConfig.java
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
import org.springframework.security.web.AuthenticationEntryPoint; // Import this
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint; // Import this
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // Import this
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
     * Defines an AuthenticationEntryPoint that returns 401 for unauthenticated API requests.
     *
     * @return An {@link AuthenticationEntryPoint} instance.
     */
    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED);
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
        requestHandler.setCsrfRequestAttributeName(null); // For compatibility with JS frameworks
        
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/", "/index.html",
                                "/static/**", "/assets/**", // Allow frontend assets
                                "/css/**", "/js/**", "/images/**",
                                "/vite.svg", "/manifest.json", "/favicon.ico",
                                "/login", "/register", // SPA routes, handled by SpaController
                                "/error" // Default error page
                        ).permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/actuator/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/register")
                        .permitAll()
                        // Public GET requests for builds and metadata
                        .requestMatchers(HttpMethod.GET, API_BUILDS_PATH,
                                "/api/builds/{identifier}",
                                "/api/builds/{identifier}/schem",
                                API_AUTHORS_PATH, API_THEMES_PATH, API_COLORS_PATH,
                                "/api/builds/query", "/api/builds/related",
                                "/api/authors/query", "/api/themes/query", "/api/colors/query"
                        )
                        .hasAnyAuthority(Role.ROLE_ADMIN.name(), Role.ROLE_USER.name(), "ROLE_ANONYMOUS") // UPDATED for guest access
                        // Authenticated endpoints
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
                        .anyRequest().authenticated() // All other requests need authentication
                )
                .userDetailsService(userDetailsService)
                .formLogin(formLogin ->
                        formLogin
                                .loginProcessingUrl(LOGIN_PROCESSING_URL) // API endpoint for login
                                .successHandler(restAuthenticationSuccessHandler)
                                .failureHandler((request, response, exception) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json");
                                    response.getWriter().append("{\"error\": "
                                                    + "\"Authentication Failed\", \"message\": \"")
                                            .append(exception.getMessage()).append("\"}");
                                })
                                .permitAll() // Allow access to the login processing URL
                )
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_PROCESSING_URL)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)
                        )
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll()
                )
                // Configure exception handling for API paths
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint(), // Use the 401 entry point
                                new AntPathRequestMatcher("/api/**") // Apply to /api/** paths
                        ));
        
        return http.build();
    }
}