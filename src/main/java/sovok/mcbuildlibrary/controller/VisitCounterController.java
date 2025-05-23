package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.service.VisitCounterService;

@RestController
@Tag(name = "Visit Counter", description = "API for tracking total API requests")
public class VisitCounterController {

    private final VisitCounterService visitCounterService;

    @Autowired
    public VisitCounterController(VisitCounterService visitCounterService) {
        this.visitCounterService = visitCounterService;
    }

    @Operation(summary = "Get Total Request Count", description = "Retrieves the total number "
            + "of requests handled by controllers since the application started (excluding "
            + "static resources, errors, swagger, and this endpoint).")
    @ApiResponse(responseCode = "200", description = "Total request count retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Map.class, example = "{\"totalRequests\": "
                            + "50}")))
    @GetMapping("/total-request-count")
    public ResponseEntity<Map<String, Integer>> getTotalRequestCount() {
        int count = visitCounterService.getTotalRequestCount();

        return ResponseEntity.ok(Map.of("totalRequests", count));
    }
}