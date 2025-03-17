package sovok.mcbuildlibrary.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sovok.mcbuildlibrary.exception.InvalidQueryParameterException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.service.AuthorService;
import sovok.mcbuildlibrary.service.BuildService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/builds")
public class BuildController {

    private final BuildService buildService;
    private final AuthorService authorService;

    public BuildController(BuildService buildService, AuthorService authorService) {
        this.buildService = buildService;
        this.authorService = authorService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> createBuild(
            @RequestParam("name") String name,
            @RequestParam("author") String authorName,
            @RequestParam("theme") String theme,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("colors") List<String> colors,
            @RequestParam(value = "screenshots", required = false) List<String> screenshots,
            @RequestParam("schemFile") MultipartFile schemFile) throws IOException {
        Author author = authorService.findOrCreateAuthor(authorName);
        Build build = Build.builder()
                .name(name)
                .author(author)
                .theme(theme)
                .description(description)
                .colors(colors)
                .screenshots(screenshots)
                .schemFile(schemFile.getBytes())
                .build();
        Build createdBuild = buildService.createBuild(build);
        return new ResponseEntity<>(createdBuild, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Build> getBuildById(@PathVariable String id) {
        try {
            Long buildId = Long.valueOf(id);
            return buildService.findBuildById(buildId)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResourceNotFoundException("No build found with ID: " + id));
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }

    @GetMapping
    public ResponseEntity<List<Build>> getAllBuilds() {
        List<Build> builds = buildService.findAll();
        if (builds == null || builds.isEmpty()) {
            throw new ResourceNotFoundException("No builds are currently available");
        }
        return ResponseEntity.ok(builds);
    }

    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQueryParams(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String theme,
            @RequestParam(value = "color", required = false) List<String> colors) {
        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, colors);
        if (filteredBuilds == null || filteredBuilds.isEmpty()) {
            throw new ResourceNotFoundException("No builds found matching the query");
        }
        return ResponseEntity.ok(filteredBuilds);
    }

    @GetMapping("/{id}/screenshot")
    public ResponseEntity<String> getScreenshot(@PathVariable String id,
                                                @RequestParam int index) {
        try {
            Long buildId = Long.valueOf(id);
            return buildService.getScreenshot(buildId, index)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> updateBuild(
            @PathVariable String id,
            @RequestParam("name") String name,
            @RequestParam("author") String authorName,
            @RequestParam("theme") String theme,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("colors") List<String> colors,
            @RequestParam(value = "screenshots", required = false) List<String> screenshots,
            @RequestParam("schemFile") MultipartFile schemFile) throws IOException {
        try {
            Long buildId = Long.valueOf(id);
            Author author = authorService.findOrCreateAuthor(authorName);
            Build updatedBuild = Build.builder()
                    .name(name)
                    .author(author)
                    .theme(theme)
                    .description(description)
                    .colors(colors)
                    .screenshots(screenshots)
                    .schemFile(schemFile.getBytes())
                    .build();
            Build build = buildService.updateBuild(buildId, updatedBuild);
            return ResponseEntity.ok(build);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuild(@PathVariable String id) {
        try {
            Long buildId = Long.valueOf(id);
            buildService.deleteBuild(buildId);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @GetMapping("/{id}/schem")
    public ResponseEntity<byte[]> getSchemFile(@PathVariable String id) {
        try {
            Long buildId = Long.valueOf(id);
            Build build = buildService.findBuildById(buildId)
                    .orElseThrow(() -> new ResourceNotFoundException("No build found with ID: " + id));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = build.getName().replaceAll("[^a-zA-Z0-9-_ ]", "") + ".schem";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(build.getSchemFile().length);

            return new ResponseEntity<>(build.getSchemFile(), headers, HttpStatus.OK);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }
}