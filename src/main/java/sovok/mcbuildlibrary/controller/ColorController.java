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
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.service.ColorService;

@RestController
@RequestMapping("/colors")
public class ColorController {

    private final ColorService colorService;

    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    @PostMapping
    public ResponseEntity<Color> createColor(@RequestParam("name") String name) {
        Color color = colorService.createColor(name);
        return new ResponseEntity<>(color, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Color>> getAllColors() {
        List<Color> colors = colorService.findAllColors();
        return ResponseEntity.ok(colors);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Color> getColorByIdentifier(@PathVariable String identifier) {
        Color color = findColorByIdentifier(identifier);
        return ResponseEntity.ok(color);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Color> updateColor(@PathVariable String identifier, @RequestParam("name")
        String newName) {
        Color color = findColorByIdentifier(identifier);
        Color updatedColor = colorService.updateColor(color.getId(), newName);
        return ResponseEntity.ok(updatedColor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteColor(@PathVariable String identifier) {
        try {
            Long colorId = Long.valueOf(identifier);
            colorService.deleteColor(colorId);
        } catch (NumberFormatException e) {
            colorService.deleteColorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<Color>> getColorsByQuery(@RequestParam(required = false)
                                                            String name) {
        List<Color> colors = colorService.findColors(name);
        if (colors.isEmpty()) {
            throw new ResourceNotFoundException("No colors found matching the query");
        }
        return ResponseEntity.ok(colors);
    }

    private Color findColorByIdentifier(String identifier) {
        try {
            Long colorId = Long.valueOf(identifier);
            return colorService.findColorById(colorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Color with ID " + identifier
                            + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        } catch (NumberFormatException e) {
            List<Color> colors = colorService.findColors(identifier);
            if (colors.isEmpty()) {
                throw new ResourceNotFoundException("Color with name " + identifier + " "
                        + ErrorMessages.NOT_FOUND_MESSAGE);
            }
            return colors.get(0);
        }
    }
}