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

    public Optional<Build> findByName(String name) {
        String pattern = "%" + name.toLowerCase() + "%";
        List<Build> builds = buildRepository.findByNameLike(pattern);
        return builds.isEmpty() ? Optional.empty() : Optional.of(builds.get(0)); // Name is unique
    }

    public List<Build> findAll() {
        return buildRepository.findAll();
    }

    public List<Build> filterBuilds(String author, String name, String theme, List<String> colors) {
        String authorPattern = author != null ? "%" + author.toLowerCase() + "%" : null;
        String namePattern = name != null ? "%" + name.toLowerCase() + "%" : null;
        String themePattern = theme != null ? "%" + theme.toLowerCase() + "%" : null;
        List<String> colorsLower = colors != null
                ? colors.stream().map(String::toLowerCase).toList() : null;
        boolean colorsEmpty = colorsLower == null || colorsLower.isEmpty();
        return buildRepository.filterBuilds(authorPattern, namePattern,
                themePattern, colorsLower, colorsEmpty);
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