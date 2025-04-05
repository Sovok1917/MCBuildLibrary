// file: src/main/java/sovok/mcbuildlibrary/controller/ThemeController.java
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
import sovok.mcbuildlibrary.dto.ThemeDto;
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.ThemeService;

@RestController
@RequestMapping("/themes")
public class ThemeController {

    private static final String IDENTIFIER_PATH_VAR = "identifier";
    private static final String NAME_REQ_PARAM = "name";

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @PostMapping
    public ResponseEntity<Theme> createTheme(@RequestParam(NAME_REQ_PARAM) String name) {
        // Service method handles cache insert
        Theme theme = themeService.createTheme(name);
        return new ResponseEntity<>(theme, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ThemeDto>> getAllThemes() {
        // Service method handles cache and initial not found
        List<ThemeDto> themes = themeService.findAllThemeDtos();
        return ResponseEntity.ok(themes);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<ThemeDto> getThemeByIdentifier(@PathVariable(IDENTIFIER_PATH_VAR) String identifier) {
        // Helper uses cached service DTO method
        ThemeDto themeDto = findThemeDtoByIdentifier(identifier);
        return ResponseEntity.ok(themeDto);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Theme> updateTheme(@PathVariable(IDENTIFIER_PATH_VAR) String identifier,
                                             @RequestParam(NAME_REQ_PARAM) String newName) {
        // Find original first (not cached) to get ID if identifier is name
        Theme theme = themeService.findThemes(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                                ErrorMessages.THEME, ErrorMessages.WITH_NAME, identifier, ErrorMessages.NOT_FOUND_MESSAGE)));

        // Service method handles cache update
        Theme updatedTheme = themeService.updateTheme(theme.getId(), newName);
        return ResponseEntity.ok(updatedTheme);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteTheme(@PathVariable(IDENTIFIER_PATH_VAR) String identifier) {
        // Service methods handle cache eviction
        try {
            Long themeId = Long.valueOf(identifier);
            themeService.deleteTheme(themeId);
        } catch (NumberFormatException e) {
            themeService.deleteThemeByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<ThemeDto>> getThemesByQuery(@RequestParam(required = false, value = NAME_REQ_PARAM)
                                                           String name) {
        // Service method (fuzzy) not cached
        List<ThemeDto> themes = themeService.findThemeDtos(name);
        if (themes.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessages.QUERY_NO_RESULTS, "themes", (name != null ? name : "<all>")));
        }
        return ResponseEntity.ok(themes);
    }

    // Helper uses cached DTO find methods from service
    private ThemeDto findThemeDtoByIdentifier(String identifier) {
        try {
            Long themeId = Long.valueOf(identifier);
            // findThemeDtoById uses cache
            return themeService.findThemeDtoById(themeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                                    ErrorMessages.THEME, ErrorMessages.WITH_ID, identifier, ErrorMessages.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            // findThemeDtos (fuzzy) is not cached, filter locally
            List<ThemeDto> themes = themeService.findThemeDtos(identifier); // Not cached
            return themes.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                                    ErrorMessages.THEME, ErrorMessages.WITH_NAME, identifier, ErrorMessages.NOT_FOUND_MESSAGE)));
        }
    }
}