package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.exception.ValidationErrorResponse;
import sovok.mcbuildlibrary.model.BaseNamedEntity;
import sovok.mcbuildlibrary.service.BaseNamedEntityService;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;

/**
 * Abstract base controller for entities extending BaseNamedEntity.
 * Provides common REST endpoints for CRUD operations and querying.
 * Subclasses must define @RequestMapping, @Tag, and inject the specific Service.
 *
 * @param <T>   The specific entity type (e.g., Author).
 * @param <D> The specific D type (e.g., AuthorDto).
 * @param <S>   The specific service type (e.g., AuthorService).
 */
@Validated
public abstract class BaseNamedEntityController<
        T extends BaseNamedEntity,
        D,
        S extends BaseNamedEntityService<T, D, ?>> {

    protected final S service;

    protected BaseNamedEntityController(S service) {
        this.service = service;
    }

    protected abstract String getEntityTypeName();

    @SuppressWarnings("unused")
    protected abstract String getEntityTypePluralName();

    @SuppressWarnings("unused")
    protected abstract String getEntityNameExample();

    @SuppressWarnings("unused")
    protected abstract String getEntityIdentifierExample();

    @Operation(summary = "Create a new entity", description = "Creates a new entity resource.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201",
            description = "Entity created successfully"), @ApiResponse(
                    responseCode = "400", description = "Invalid input (blank name, "
            + "duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail
                                    .class})))
    })
    @PostMapping
    public ResponseEntity<T> createEntity(
            @Parameter(description = "Name of the new entity", required = true)
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String name) {
        T entity = service.create(name);
        return new ResponseEntity<>(entity, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all entities", description = "Retrieves a list of all entities with "
            + "their related builds.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved entities")
    })
    @GetMapping
    public ResponseEntity<List<D>> getAllEntities() {
        List<D> dtos = service.findAllDtos();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get entity by identifier", description = "Retrieves a specific "
            + "entity by its ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved entity"), @ApiResponse(responseCode = "404",
            description = "Entity not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{identifier}")
    public ResponseEntity<D> getEntityByIdentifier(
            @Parameter(description = "ID or exact name of the entity", required = true)
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        D dto = findDtoByIdentifier(identifier); // Use the fixed helper
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Update an entity's name", description = "Updates the name of an "
            + "existing entity identified by ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Entity updated successfully"), @ApiResponse(responseCode = "400",
            description = "Invalid input (blank name, "
                    + "duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class,
                                ProblemDetail.class}))), @ApiResponse(responseCode = "404",
            description = "Entity not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{identifier}")
    public ResponseEntity<T> updateEntity(
            @Parameter(description = "ID or exact name of the entity to update", required = true)
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @Parameter(description = "The new name for the entity", required = true)
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String newName) {
        T entityToUpdate = findEntityByIdentifier(identifier);
        T updatedEntity = service.update(entityToUpdate.getId(), newName);
        return ResponseEntity.ok(updatedEntity);
    }

    @Operation(summary = "Delete an entity", description = "Deletes an entity by ID or exact "
            + "name. Fails if constraints are violated (e.g., associated builds for Theme/Color).")
    @ApiResponses(value = {@ApiResponse(responseCode = "204",
            description = "Entity deleted successfully", content = @Content), @ApiResponse(
                    responseCode = "404", description = "Entity not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(
                                    responseCode = "409", description = "Entity cannot be deleted "
            + "due to associations",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteEntity(
            @Parameter(description = "ID or exact name of the entity to delete", required = true)
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        try {
            Long entityId = Long.valueOf(identifier);
            service.deleteById(entityId);
        } catch (NumberFormatException e) {
            service.deleteByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Find entities by name query", description = "Finds entities using a "
            + "case-insensitive fuzzy name match (via SIMILARITY).")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully found "
            + "entities (list might be empty)"), @ApiResponse(responseCode = "400", description
            = "Invalid query parameter provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/query")
    public ResponseEntity<List<D>> getEntitiesByQuery(
            @Parameter(description = "Fuzzy name to search for entities.")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false) String name) {
        List<D> dtos = service.findDtosByNameQuery(name);
        return ResponseEntity.ok(dtos);
    }

    // --- Helper Methods ---

    /**
     * Finds the entity D by ID or name. Throws NoSuchElementException if not found.
     *
     * @param identifier ID or name.
     * @return The found D.
     */
    protected D findDtoByIdentifier(String identifier) {
        try {
            Long entityId = Long.valueOf(identifier);
            return service.findDtoById(entityId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    getEntityTypeName(), StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            // *** FIX: Use the public convertToDto method from the service ***
            T entity = service.findByName(identifier)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    getEntityTypeName(), StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
            // This call is now valid because convertToDto is public in the service
            return service.convertToDto(entity);
        }
    }

    /**
     * Finds the raw entity T by ID or name. Throws NoSuchElementException if not found.
     * Useful for update/delete operations that need the entity itself.
     *
     * @param identifier ID or name.
     * @return The found entity T.
     */
    protected T findEntityByIdentifier(String identifier) {
        try {
            Long entityId = Long.valueOf(identifier);
            return service.findById(entityId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    getEntityTypeName(), StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            return service.findByName(identifier)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    getEntityTypeName(), StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}