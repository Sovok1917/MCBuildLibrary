package sovok.mcbuildlibrary.service;

import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.dao.BuildDao;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;

import java.util.List;
import java.util.Optional;

@Service
public class BuildService {

    private final BuildDao buildDao;

    public BuildService(BuildDao buildDao) {
        this.buildDao = buildDao;
    }

    public Build createBuild(Build build) {
        return buildDao.save(build);
    }

    public Optional<Build> findBuildById(Long id) {
        return buildDao.findById(id);
    }

    public List<Build> findAll() {
        return buildDao.findAll();
    }

    public List<Build> filterBuilds(String author, String name, String theme, List<String> colors) {
        boolean colorsEmpty = colors == null || colors.isEmpty();
        return buildDao.filterBuilds(author, name, theme, colors, colorsEmpty);
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
        return buildDao.findById(id)
                .map(existingBuild -> {
                    existingBuild.setName(updatedBuild.getName());
                    existingBuild.setAuthor(updatedBuild.getAuthor());
                    existingBuild.setTheme(updatedBuild.getTheme());
                    existingBuild.setDescription(updatedBuild.getDescription());
                    existingBuild.setColors(updatedBuild.getColors());
                    existingBuild.setScreenshots(updatedBuild.getScreenshots());
                    existingBuild.setSchemFile(updatedBuild.getSchemFile());
                    return buildDao.save(existingBuild);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Build with ID " + id + " not found"));
    }

    public void deleteBuild(Long id) {
        if (!buildDao.existsById(id)) {
            throw new ResourceNotFoundException("Build with ID " + id + " not found");
        }
        buildDao.deleteById(id);
    }
}