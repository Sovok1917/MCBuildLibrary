// file: src/main/java/sovok/mcbuildlibrary/service/BuildService.java
package sovok.mcbuildlibrary.service;

import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;

import java.util.List;
import java.util.Optional;

@Service
public class BuildService {

    private final BuildRepository buildRepository;

    public BuildService(BuildRepository buildRepository) {
        this.buildRepository = buildRepository;
    }

    public Build createBuild(Build build) {
        return buildRepository.save(build);
    }

    public Optional<Build> findBuildById(Long id) {
        return buildRepository.findById(id);
    }

    public List<Build> findAll() {
        return buildRepository.findAll();
    }

    public List<Build> filterBuilds(String author, String name, String theme, List<String> colors) {
        boolean colorsEmpty = colors == null || colors.isEmpty();
        return buildRepository.filterBuilds(author, name, theme, colors, colorsEmpty);
    }

    public Optional<String> getScreenshot(Long id, int index) {
        return findBuildById(id)
                .flatMap(build -> {
                    if (index < 0 || index >= build.getScreenshots().size()) {
                        return Optional.empty();
                    }
                    return Optional.of(build.getScreenshots().get(index));
                });
    }

    public Build updateBuild(Long id, Build updatedBuild) {
        return buildRepository.findById(id)
                .map(existingBuild -> {
                    existingBuild.setName(updatedBuild.getName());
                    existingBuild.setAuthors(updatedBuild.getAuthors());
                    existingBuild.setThemes(updatedBuild.getThemes());
                    existingBuild.setDescription(updatedBuild.getDescription());
                    existingBuild.setColors(updatedBuild.getColors());
                    existingBuild.setScreenshots(updatedBuild.getScreenshots());
                    existingBuild.setSchemFile(updatedBuild.getSchemFile());
                    return buildRepository.save(existingBuild);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Build with ID " + id + " not found"));
    }

    public void deleteBuild(Long id) {
        if (!buildRepository.existsById(id)) {
            throw new ResourceNotFoundException("Build with ID " + id + " not found");
        }
        buildRepository.deleteById(id);
    }
}