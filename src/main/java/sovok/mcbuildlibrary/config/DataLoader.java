package sovok.mcbuildlibrary.config;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sovok.mcbuildlibrary.model.Role;
import sovok.mcbuildlibrary.model.User;
import sovok.mcbuildlibrary.repository.UserRepository;

/**
 * Loads initial data into the database upon application startup.
 * Creates a default admin user using credentials provided via application properties.
 * This component is active by default but can be excluded by profiles if needed.
 * For example, add @Profile("!prod") to disable in a 'prod' profile.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.username:admin}") // Default to "admin" if property not set
    private String defaultAdminUsername;

    // This value MUST be provided, e.g., via environment variable or a secure properties file.
    @Value("${app.default-admin.password}")
    private String defaultAdminPassword;

    // Placeholder value to check against, ensuring the actual password is set.
    private static final String PASSWORD_PLACEHOLDER =
        "YOUR_STRONG_ADMIN_PASSWORD_HERE_ENV_OR_CONFIG";

    /**
   * Constructs the DataLoader.
   *
   * @param userRepository  The repository for user data.
   * @param passwordEncoder The encoder for hashing passwords.
   */
    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
  
    @Override
    public void run(String... args) {
        if (defaultAdminPassword == null || defaultAdminPassword.isBlank()
                || defaultAdminPassword.equals(PASSWORD_PLACEHOLDER)) {
            logger.error(
                "Default admin password ('app.default-admin.password') is not configured or is "
                    + "still set to the placeholder value ({}). "
                    + "Please set it via environment variables or application properties. "
                    + "Skipping default admin user creation.", PASSWORD_PLACEHOLDER);
            return;
        }
        
        if (defaultAdminUsername == null || defaultAdminUsername.isBlank()) {
            logger.error(
                  "Default admin username ('app.default-admin.username') is "
                          + "not configured or is empty. "
                          + "Skipping default admin user creation.");
            return;
        }
        createDefaultAdminUser();
    }
  
    private void createDefaultAdminUser() {
        if (userRepository.findByUsername(defaultAdminUsername).isEmpty()) {
            User adminUser = User.builder()
                .username(defaultAdminUsername)
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER))
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();
            userRepository.save(adminUser);
            logger.info("Created default admin user: {}", defaultAdminUsername);
            logger.warn(
                "Default admin user '{}' created using password from application properties/env. "
                        + "Ensure this is managed securely.",
                defaultAdminUsername);
        } else {
            logger.info("Admin user {} already exists. No action taken.", defaultAdminUsername);
        }
    }
}