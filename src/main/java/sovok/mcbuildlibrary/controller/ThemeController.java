// file: src/main/java/sovok/mcbuildlibrary/controller/ThemeController.java
package sovok.mcbuildlibrary.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sovok.mcbuildlibrary.exception.InvalidQueryParameterException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.ThemeService;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<Theme> getThemeById(@PathVariable String id) {
        try {
            Long themeId = Long.valueOf(id);
            Theme theme = themeService.findThemeById(themeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + id + " not found"));
            return ResponseEntity.ok(theme);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Theme> updateTheme(@PathVariable String id, @RequestParam("name") String name) {
        try {
            Long themeId = Long.valueOf(id);
            Theme updatedTheme = themeService.updateTheme(themeId, name);
            return ResponseEntity.ok(updatedTheme);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheme(@PathVariable String id) {
        try {
            Long themeId = Long.valueOf(id);
            themeService.deleteTheme(themeId);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }
}