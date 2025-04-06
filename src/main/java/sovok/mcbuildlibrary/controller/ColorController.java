// file: src/main/java/sovok/mcbuildlibrary/controller/ColorController.java
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
import sovok.mcbuildlibrary.dto.ColorDto;
// Removed import for ResourceNotFoundException
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.service.ColorService;

@RestController
@RequestMapping("/colors")
@Validated
public class ColorController {

    private final ColorService colorService;

    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    @PostMapping
    public ResponseEntity<Color> createColor(
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String name) {
        // Service throws IllegalArgumentException if duplicate
        Color color = colorService.createColor(name);
        return new ResponseEntity<>(color, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ColorDto>> getAllColors() {
        List<ColorDto> colors = colorService.findAllColorDtos();
        return ResponseEntity.ok(colors);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<ColorDto> getColorByIdentifier(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Helper throws NoSuchElementException
        ColorDto colorDto = findColorDtoByIdentifier(identifier);
        return ResponseEntity.ok(colorDto);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Color> updateColor(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String newName) {
        // Service methods handle not found / duplicate checks
        Color color = colorService.findColors(identifier).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                StringConstants.COLOR, StringConstants.WITH_NAME, identifier,
                                StringConstants.NOT_FOUND_MESSAGE)));

        Color updatedColor = colorService.updateColor(color.getId(), newName);
        return ResponseEntity.ok(updatedColor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteColor(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Service methods handle not found / conflict checks
        try {
            Long colorId = Long.valueOf(identifier);
            colorService.deleteColor(colorId);
        } catch (NumberFormatException e) {
            colorService.deleteColorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<ColorDto>> getColorsByQuery(
            @RequestParam Map<String, String> allParams) {

        validateQueryParameters(allParams, StringConstants.ALLOWED_SIMPLE_QUERY_PARAMS);
        String name = allParams.get(StringConstants.NAME_REQ_PARAM);

        List<ColorDto> colors = colorService.findColorDtos(name);
        // Don't throw 404 for empty query results, return OK
        return ResponseEntity.ok(colors);
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

    private ColorDto findColorDtoByIdentifier(String identifier) {
        try {
            Long colorId = Long.valueOf(identifier);
            return colorService.findColorDtoById(colorId)
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.COLOR, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            List<ColorDto> colors = colorService.findColorDtos(identifier);
            return colors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.COLOR, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}