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

    @PostMapping
    public ResponseEntity<Theme> createTheme(@RequestParam("name") String name) {
        Theme theme = themeService.createTheme(name);
        return new ResponseEntity<>(theme, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Theme>> getAllThemes() {
        List<Theme> themes = themeService.findAllThemes();
        return ResponseEntity.ok(themes);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Theme> getThemeByIdentifier(@PathVariable String identifier) {
        Theme theme = findThemeByIdentifier(identifier);
        return ResponseEntity.ok(theme);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Theme> updateTheme(@PathVariable String identifier, @RequestParam("name")
        String newName) {
        Theme theme = findThemeByIdentifier(identifier);
        Theme updatedTheme = themeService.updateTheme(theme.getId(), newName);
        return ResponseEntity.ok(updatedTheme);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteTheme(@PathVariable String identifier) {
        try {
            Long themeId = Long.valueOf(identifier);
            themeService.deleteTheme(themeId);
        } catch (NumberFormatException e) {
            themeService.deleteThemeByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<Theme>> getThemesByQuery(@RequestParam(required = false)
                                                            String name) {
        List<Theme> themes = themeService.findThemes(name);
        if (themes.isEmpty()) {
            throw new ResourceNotFoundException("No themes found matching the query");
        }
        return ResponseEntity.ok(themes);
    }

    private Theme findThemeByIdentifier(String identifier) {
        try {
            Long themeId = Long.valueOf(identifier);
            return themeService.findThemeById(themeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + identifier
                            + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        } catch (NumberFormatException e) {
            List<Theme> themes = themeService.findThemes(identifier);
            if (themes.isEmpty()) {
                throw new ResourceNotFoundException("Theme with name " + identifier + " "
                        + ErrorMessages.NOT_FOUND_MESSAGE);
            }
            return themes.get(0);
        }
    }
}