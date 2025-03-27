package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class BuildService {

    private final BuildRepository buildRepository;

    public BuildService(BuildRepository buildRepository) {
        this.buildRepository = buildRepository;
    }

    public Build createBuild(Build build) {
        // Check if a build with the same name already exists
        Optional<Build> existingBuild = buildRepository.findByName(build.getName());
        if (existingBuild.isPresent()) {
            throw new EntityInUseException("A build with the name '" + build.getName()
                    + "' already exists. Please choose a unique name.");
        }
        return buildRepository.save(build);
    }

    public Optional<Build> findBuildById(Long id) {
        return buildRepository.findById(id);
    }

    // Add this method to check for existing builds by name
    public Optional<Build> findByName(String name) {
        return buildRepository.findByName(name);
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
                    // Check if the new name is already taken by another build
                    Optional<Build> buildWithSameName
                            = buildRepository.findByName(updatedBuild.getName());
                    if (buildWithSameName.isPresent()
                            && !buildWithSameName.get().getId().equals(id)) {
                        throw new EntityInUseException("A build with the name '"
                                + updatedBuild.getName()
                                + "' already exists. Please choose a unique name.");
                    }
                    existingBuild.setName(updatedBuild.getName());
                    existingBuild.setAuthors(updatedBuild.getAuthors());
                    existingBuild.setThemes(updatedBuild.getThemes());
                    existingBuild.setDescription(updatedBuild.getDescription());
                    existingBuild.setColors(updatedBuild.getColors());
                    existingBuild.setScreenshots(updatedBuild.getScreenshots());
                    existingBuild.setSchemFile(updatedBuild.getSchemFile());
                    return buildRepository.save(existingBuild);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Build with ID " + id
                        + " not found"));
    }

    public void deleteBuild(Long id) {
        if (!buildRepository.existsById(id)) {
            throw new ResourceNotFoundException("Build with ID " + id + " not found");
        }
        buildRepository.deleteById(id);
    }
}