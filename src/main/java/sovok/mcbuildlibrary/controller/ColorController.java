// file: src/main/java/sovok/mcbuildlibrary/controller/ColorController.java
package sovok.mcbuildlibrary.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sovok.mcbuildlibrary.exception.InvalidQueryParameterException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.service.ColorService;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<Color> getColorById(@PathVariable String id) {
        try {
            Long colorId = Long.valueOf(id);
            Color color = colorService.findColorById(colorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Color with ID " + id + " not found"));
            return ResponseEntity.ok(color);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Color> updateColor(@PathVariable String id, @RequestParam("name") String name) {
        try {
            Long colorId = Long.valueOf(id);
            Color updatedColor = colorService.updateColor(colorId, name);
            return ResponseEntity.ok(updatedColor);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColor(@PathVariable String id) {
        try {
            Long colorId = Long.valueOf(id);
            colorService.deleteColor(colorId);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }
}