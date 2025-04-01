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
import sovok.mcbuildlibrary.dto.ColorDto; // Import DTO
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.service.ColorService;

@RestController
@RequestMapping("/colors")
public class ColorController {

    private final ColorService colorService;

    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    // Create still returns the basic Color entity
    @PostMapping
    public ResponseEntity<Color> createColor(@RequestParam("name") String name) {
        Color color = colorService.createColor(name);
        return new ResponseEntity<>(color, HttpStatus.CREATED);
    }

    // --- Modified to return List<ColorDto> ---
    @GetMapping
    public ResponseEntity<List<ColorDto>> getAllColors() {
        List<ColorDto> colors = colorService.findAllColorDtos();
        return ResponseEntity.ok(colors);
    }
    // --- End Modification ---

    // --- Modified to return ColorDto ---
    @GetMapping("/{identifier}")
    public ResponseEntity<ColorDto> getColorByIdentifier(@PathVariable String identifier) {
        ColorDto colorDto = findColorDtoByIdentifier(identifier);
        return ResponseEntity.ok(colorDto);
    }
    // --- End Modification ---

    // Update still returns the basic Color entity
    @PutMapping("/{identifier}")
    public ResponseEntity<Color> updateColor(@PathVariable String identifier, @RequestParam("name")
        String newName) {
        // Find the original color first to get ID
        Color color = colorService.findColors(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Color " + identifier + " "
                        + ErrorMessages.NOT_FOUND_MESSAGE)); // Simplified find logic

        Color updatedColor = colorService.updateColor(color.getId(), newName);
        return ResponseEntity.ok(updatedColor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteColor(@PathVariable String identifier) {
        // Deletion logic remains the same
        try {
            Long colorId = Long.valueOf(identifier);
            colorService.deleteColor(colorId);
        } catch (NumberFormatException e) {
            colorService.deleteColorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    // --- Modified to return List<ColorDto> ---
    @GetMapping("/query")
    public ResponseEntity<List<ColorDto>> getColorsByQuery(@RequestParam(required = false) String
                                                                       name) {
        List<ColorDto> colors = colorService.findColorDtos(name);
        if (colors.isEmpty()) {
            // Check for empty list here
            throw new ResourceNotFoundException("No colors found matching the query: "
                    + (name != null ? name : "<all>"));
        }
        return ResponseEntity.ok(colors);
    }
    // --- End Modification ---

    // --- Helper modified to return ColorDto and use DTO service methods ---
    private ColorDto findColorDtoByIdentifier(String identifier) {
        try {
            Long colorId = Long.valueOf(identifier);
            return colorService.findColorDtoById(colorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Color with ID " + identifier
                            + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        } catch (NumberFormatException e) {
            // Treat as name
            List<ColorDto> colors = colorService.findColorDtos(identifier);
            if (colors.isEmpty()) {
                throw new ResourceNotFoundException("Color with name '" + identifier + "' "
                        + ErrorMessages.NOT_FOUND_MESSAGE);
            }
            return colors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Color with name '"
                            + identifier + "' "
                            + ErrorMessages.NOT_FOUND_MESSAGE));
        }
    }
    // --- End Modification ---
}