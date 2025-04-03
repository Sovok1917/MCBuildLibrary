package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public Build createBuild(Build build) {
        Optional<Build> existingBuild = buildRepository.findByName(build.getName());
        if (existingBuild.isPresent()) {
            throw new EntityInUseException("A build with the name '" + build.getName()
                    + "' already exists. Please choose a unique name.");
        }
        return buildRepository.save(build);
    }

    // Removed @Transactional since it's always called within a transactional context
    public Optional<Build> findBuildById(Long id) {
        return buildRepository.findById(id);
    }

    // Removed @Transactional since it's called within a transactional context
    public Optional<Build> findByName(String name) {
        String pattern = "%" + name.toLowerCase() + "%";
        List<Build> builds = buildRepository.findByNameLike(pattern);
        return builds.isEmpty() ? Optional.empty() : Optional.of(builds.get(0));
    }

    @Transactional(readOnly = true)
    public List<Build> findAll() {
        return buildRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Build> filterBuilds(String author, String name, String theme, List<String> colors) {
        String colorsStr = (colors != null && !colors.isEmpty()) ? String.join(",", colors) : null;
        return buildRepository.fuzzyFilterBuilds(author, name, theme, colorsStr);
    }

    @Transactional(readOnly = true)
    public Optional<String> getScreenshot(Long id, int index) {
        return findBuildById(id)
                .flatMap(build -> {
                    if (index < 0 || index >= build.getScreenshots().size()) {
                        return Optional.empty();
                    }
                    return Optional.of(build.getScreenshots().get(index));
                });
    }

    @Transactional
    public Build updateBuild(Long id, Build updatedBuild) {
        return findBuildById(id)
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

    @Transactional
    public void deleteBuild(Long id) {
        if (!buildRepository.existsById(id)) {
            throw new ResourceNotFoundException("Build with ID " + id + " not found");
        }
        buildRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> getSchemFile(Long id) {
        return findBuildById(id)
                .map(build -> {
                    byte[] schemFile = build.getSchemFile();
                    return schemFile != null ? Optional.of(schemFile) : Optional.<byte[]>empty();
                })
                .orElse(Optional.empty());
    }
}