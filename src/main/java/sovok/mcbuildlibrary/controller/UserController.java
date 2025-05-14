package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors; // Added import for Collectors
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.dto.UserRegistrationDto;
import sovok.mcbuildlibrary.exception.ValidationErrorResponse;
import sovok.mcbuildlibrary.model.User;
import sovok.mcbuildlibrary.service.UserService;

/**
 * Controller for user-related operations, such as fetching current user details and registration.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User-related operations")
public class UserController {
    
    private final UserService userService;
    
    /**
     * Constructs the UserController.
     *
     * @param userService Service for user operations like registration.
     */
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Simple DTO for Swagger documentation and API responses for user information.
     * Made public static to be correctly exposed.
     */
    @Schema(name = "UserInfo", description = "Information about the authenticated or"
            + " registered user")
    public static class UserInfo { // Changed from private to public
        @Schema(example = "sampleUser")
        public String username;
        
        @Schema(example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
        public Set<String> authorities;
    }
    
    /**
     * Retrieves the details of the currently authenticated user.
     *
     * @param principal The authenticated principal (UserDetails).
     * @return A ResponseEntity containing user details (username, roles) or 401 if not
     *         authenticated.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieves details of the currently"
            + " authenticated user.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved current user",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserInfo.class))) // UserInfo is now public
    @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content)
    public ResponseEntity<Object> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        // Constructing UserInfo directly for type safety and clarity in response
        UserInfo userInfoResponse = new UserInfo();
        userInfoResponse.username = principal.getUsername();
        userInfoResponse.authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(userInfoResponse);
    }
    
    /**
     * Registers a new user.
     *
     * @param registrationDto DTO containing username and password.
     * @return ResponseEntity with the created User details (excluding password) or error.
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account.")
    @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserInfo.class))) // UserInfo is now public
    @ApiResponse(responseCode = "400", description = "Invalid input (validation errors, "
            + "username exists)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail.class})))
    public ResponseEntity<UserInfo> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationDto) {
        User newUser = userService.registerUser(registrationDto);
        
        UserInfo registeredUserInfo = new UserInfo();
        registeredUserInfo.username = newUser.getUsername();
        registeredUserInfo.authorities = newUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        
        return new ResponseEntity<>(registeredUserInfo, HttpStatus.CREATED);
    }
}