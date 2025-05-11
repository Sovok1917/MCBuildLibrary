package sovok.mcbuildlibrary.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.repository.UserRepository;

/**
 * Implements Spring Security's {@link UserDetailsService} to load user-specific data.
 * It retrieves user details from the {@link UserRepository}.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
   * Constructs the UserDetailsServiceImpl.
   *
   * @param userRepository The repository for accessing user data.
   */
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
   * Loads a user by their username.
   *
   * @param username The username of the user to load.
   * @return The {@link UserDetails} for the found user.
   * @throws UsernameNotFoundException if the user with the given username is not found.
   */

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
            "User not found with username: " + username));
    // The User entity itself implements UserDetails, so we can return it directly.
    // Ensure roles are eagerly fetched or fetched within the transaction.
    // The User entity has FetchType.EAGER for roles, so this is fine.
    }
}