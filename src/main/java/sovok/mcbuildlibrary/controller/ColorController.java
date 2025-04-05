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
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.service.ColorService;

@RestController
@RequestMapping("/colors")
public class ColorController {

    private static final String IDENTIFIER_PATH_VAR = "identifier";
    private static final String NAME_REQ_PARAM = "name";

    private final ColorService colorService;

    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    @PostMapping
    public ResponseEntity<Color> createColor(@RequestParam(NAME_REQ_PARAM) String name) {
        // Service method handles cache insert
        Color color = colorService.createColor(name);
        return new ResponseEntity<>(color, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ColorDto>> getAllColors() {
        // Service method handles cache and initial not found
        List<ColorDto> colors = colorService.findAllColorDtos();
        return ResponseEntity.ok(colors);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<ColorDto> getColorByIdentifier(@PathVariable(IDENTIFIER_PATH_VAR)
                                                             String identifier) {
        // Helper uses cached service DTO method
        ColorDto colorDto = findColorDtoByIdentifier(identifier);
        return ResponseEntity.ok(colorDto);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Color> updateColor(@PathVariable(IDENTIFIER_PATH_VAR) String identifier,
                                             @RequestParam(NAME_REQ_PARAM) String newName) {
        Color color = colorService.findColors(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                StringConstants.COLOR, StringConstants.WITH_NAME, identifier,
                                StringConstants.NOT_FOUND_MESSAGE)));

        // Service method handles cache update
        Color updatedColor = colorService.updateColor(color.getId(), newName);
        return ResponseEntity.ok(updatedColor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteColor(@PathVariable(IDENTIFIER_PATH_VAR) String identifier) {
        // Service methods handle cache eviction
        try {
            Long colorId = Long.valueOf(identifier);
            colorService.deleteColor(colorId);
        } catch (NumberFormatException e) {
            colorService.deleteColorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<ColorDto>> getColorsByQuery(@RequestParam(required = false,
            value = NAME_REQ_PARAM)
                                                           String name) {
        // Service method (fuzzy) not cached
        List<ColorDto> colors = colorService.findColorDtos(name);
        if (colors.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(StringConstants.QUERY_NO_RESULTS, "colors",
                            (name != null ? name : "<all>")));
        }
        return ResponseEntity.ok(colors);
    }

    // Helper uses cached DTO find methods from service
    private ColorDto findColorDtoByIdentifier(String identifier) {
        try {
            Long colorId = Long.valueOf(identifier);
            // findColorDtoById uses cache
            return colorService.findColorDtoById(colorId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.COLOR, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            // findColorDtos (fuzzy) is not cached, filter locally
            List<ColorDto> colors = colorService.findColorDtos(identifier); // Not cached
            return colors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.COLOR, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}