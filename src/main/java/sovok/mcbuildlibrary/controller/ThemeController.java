// file: src/main/java/sovok/mcbuildlibrary/controller/ThemeController.java
package sovok.mcbuildlibrary.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException; // Import
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.dto.ThemeDto;
// Removed import for ResourceNotFoundException
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.ThemeService;

@RestController
@RequestMapping("/themes")
@Validated
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @PostMapping
    public ResponseEntity<Theme> createTheme(
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String name) {
        // Service throws IllegalArgumentException if duplicate
        Theme theme = themeService.createTheme(name);
        return new ResponseEntity<>(theme, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ThemeDto>> getAllThemes() {
        List<ThemeDto> themes = themeService.findAllThemeDtos();
        return ResponseEntity.ok(themes);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<ThemeDto> getThemeByIdentifier(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Helper throws NoSuchElementException
        ThemeDto themeDto = findThemeDtoByIdentifier(identifier);
        return ResponseEntity.ok(themeDto);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Theme> updateTheme(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String newName) {
        // Service handles not found / duplicate checks
        Theme theme = themeService.findThemes(identifier).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                StringConstants.THEME, StringConstants.WITH_NAME, identifier,
                                StringConstants.NOT_FOUND_MESSAGE)));

        Theme updatedTheme = themeService.updateTheme(theme.getId(), newName);
        return ResponseEntity.ok(updatedTheme);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteTheme(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Service handles not found / conflict checks
        try {
            Long themeId = Long.valueOf(identifier);
            themeService.deleteTheme(themeId);
        } catch (NumberFormatException e) {
            themeService.deleteThemeByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<ThemeDto>> getThemesByQuery(
            @RequestParam Map<String, String> allParams) {

        validateQueryParameters(allParams, StringConstants.ALLOWED_SIMPLE_QUERY_PARAMS);
        String name = allParams.get(StringConstants.NAME_REQ_PARAM);

        List<ThemeDto> themes = themeService.findThemeDtos(name);
        // Return 200 OK with empty list if no results
        return ResponseEntity.ok(themes);
    }

    private void validateQueryParameters(Map<String, String> receivedParams, Set<String> allowedParams) {
        for (String paramName : receivedParams.keySet()) {
            if (!allowedParams.contains(paramName)) {
                // Throw IllegalArgumentException for invalid query parameter
                throw new IllegalArgumentException(
                        String.format(StringConstants.INVALID_QUERY_PARAMETER_DETECTED,
                                paramName,
                                String.join(", ", allowedParams.stream().sorted().toList()))
                );
            }
        }
    }

    private ThemeDto findThemeDtoByIdentifier(String identifier) {
        try {
            Long themeId = Long.valueOf(identifier);
            return themeService.findThemeDtoById(themeId)
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.THEME, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            List<ThemeDto> themes = themeService.findThemeDtos(identifier);
            return themes.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.THEME, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}