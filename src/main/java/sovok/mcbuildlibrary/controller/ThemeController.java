package sovok.mcbuildlibrary.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.dto.ThemeDto; // Import DTO
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.ThemeService;

@RestController
@RequestMapping("/themes")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    // Create still returns the basic Theme entity
    @PostMapping
    public ResponseEntity<Theme> createTheme(@RequestParam("name") String name) {
        Theme theme = themeService.createTheme(name);
        return new ResponseEntity<>(theme, HttpStatus.CREATED);
    }

    // --- Modified to return List<ThemeDto> ---
    @GetMapping
    public ResponseEntity<List<ThemeDto>> getAllThemes() {
        List<ThemeDto> themes = themeService.findAllThemeDtos();
        return ResponseEntity.ok(themes);
    }
    // --- End Modification ---

    // --- Modified to return ThemeDto ---
    @GetMapping("/{identifier}")
    public ResponseEntity<ThemeDto> getThemeByIdentifier(@PathVariable String identifier) {
        ThemeDto themeDto = findThemeDtoByIdentifier(identifier);
        return ResponseEntity.ok(themeDto);
    }
    // --- End Modification ---

    // Update still returns the basic Theme entity
    @PutMapping("/{identifier}")
    public ResponseEntity<Theme> updateTheme(@PathVariable String identifier, @RequestParam("name")
        String newName) {
        // Find the original theme first to get ID
        Theme theme = themeService.findThemes(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Theme " + identifier + " "
                        + ErrorMessages.NOT_FOUND_MESSAGE)); // Simplified find logic

        Theme updatedTheme = themeService.updateTheme(theme.getId(), newName);
        return ResponseEntity.ok(updatedTheme);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteTheme(@PathVariable String identifier) {
        // Deletion logic remains the same
        try {
            Long themeId = Long.valueOf(identifier);
            themeService.deleteTheme(themeId);
        } catch (NumberFormatException e) {
            themeService.deleteThemeByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    // --- Modified to return List<ThemeDto> ---
    @GetMapping("/query")
    public ResponseEntity<List<ThemeDto>> getThemesByQuery(@RequestParam(required = false)
                                                               String name) {
        List<ThemeDto> themes = themeService.findThemeDtos(name);
        if (themes.isEmpty()) {
            // Check for empty list here
            throw new ResourceNotFoundException("No themes found matching the query: "
                    + (name != null ? name : "<all>"));
        }
        return ResponseEntity.ok(themes);
    }
    // --- End Modification ---

    // --- Helper modified to return ThemeDto and use DTO service methods ---
    private ThemeDto findThemeDtoByIdentifier(String identifier) {
        try {
            Long themeId = Long.valueOf(identifier);
            return themeService.findThemeDtoById(themeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + identifier
                            + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        } catch (NumberFormatException e) {
            // Treat as name
            List<ThemeDto> themes = themeService.findThemeDtos(identifier);
            if (themes.isEmpty()) {
                throw new ResourceNotFoundException("Theme with name '" + identifier + "' "
                        + ErrorMessages.NOT_FOUND_MESSAGE);
            }
            return themes.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Theme with name '"
                            + identifier + "' "
                            + ErrorMessages.NOT_FOUND_MESSAGE));
        }
    }
    // --- End Modification ---
}