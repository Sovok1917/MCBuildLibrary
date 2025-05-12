package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user-related operations, such as fetching current user details.
 */
@RestController
@RequestMapping("/users") // CORRECTED MAPPING
@Tag(name = "Users", description = "User-related operations")
public class UserController {
    
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
                    schema = @Schema(implementation = UserInfo.class)))
    @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content)
    public ResponseEntity<Object> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            // For SPAs, it's often better to return 401 if not authenticated,
            // rather than a 200 with an error body, as the HTTP status itself is indicative.
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        Map<String, Object> userInfo = Map.of(
                "username", principal.getUsername(),
                "authorities", principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                                .toList()
        );
        return ResponseEntity.ok(userInfo);
    }
    
    // Simple DTO for Swagger documentation of the /me endpoint response
    @Schema(name = "UserInfo", description = "Information about the authenticated user")
    private static class UserInfo {
        @Schema(example = "admin")
        public String username;
        @Schema(example = "[\"ROLE_ADMIN\", \"ROLE_USER\"]")
        public Set<String> authorities; // Keep as Set if that's what you intend
    }
}