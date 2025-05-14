package sovok.mcbuildlibrary.service;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.dto.UserRegistrationDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Role;
import sovok.mcbuildlibrary.model.User;
import sovok.mcbuildlibrary.repository.UserRepository;

/**
 * Service layer for managing User entities, including registration.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs the UserService.
     *
     * @param userRepository  The repository for user data access.
     * @param passwordEncoder The encoder for hashing passwords.
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user in the system.
     * Checks for existing username, encodes the password, assigns default role, and saves the user.
     *
     * @param registrationDto DTO containing the username and password for the new user.
     * @return The newly created and saved User entity.
     * @throws IllegalArgumentException if the username already exists.
     */
    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.findByUsername(registrationDto.username()).isPresent()) {
            throw new IllegalArgumentException(
                String.format(StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    "User", StringConstants.WITH_NAME, registrationDto.username(),
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        User newUser = User.builder()
            .username(registrationDto.username())
            .password(passwordEncoder.encode(registrationDto.password()))
            .roles(Set.of(Role.ROLE_USER)) // Assign default role
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

        User savedUser = userRepository.save(newUser);
        logger.info("Registered new user: {}", savedUser.getUsername());
        return savedUser;
    }
}