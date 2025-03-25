package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ThemeRepository;

@Service
public class ThemeService {
    private final ThemeRepository themeRepository;
    private final BuildRepository buildRepository;

    public ThemeService(ThemeRepository themeRepository, BuildRepository buildRepository) {
        this.themeRepository = themeRepository;
        this.buildRepository = buildRepository;
    }

    public Theme findOrCreateTheme(String name) {
        Optional<Theme> themeOpt = themeRepository.findByName(name);
        return themeOpt.orElseGet(() -> {
            Theme newTheme = Theme.builder().name(name).build();
            return themeRepository.save(newTheme);
        });
    }

    public Theme createTheme(String name) {
        if (themeRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException(
                    "Theme with name '" + name + "' already exists");
        }
        Theme theme = Theme.builder().name(name).build();
        return themeRepository.save(theme);
    }

    public Optional<Theme> findThemeById(Long id) {
        return themeRepository.findById(id);
    }

    public List<Theme> findAllThemes() {
        List<Theme> themes = themeRepository.findAll();
        if (themes.isEmpty()) {
            throw new ResourceNotFoundException("No themes are currently available");
        }
        return themes;
    }

    public Theme updateTheme(Long id, String newName) {
        return themeRepository.findById(id)
                .map(theme -> {
                    if (themeRepository.findByName(newName).isPresent()
                            && !theme.getName().equals(newName)) {
                        throw new IllegalArgumentException(
                                "Another theme with name '" + newName + "' already exists");
                    }
                    theme.setName(newName);
                    return themeRepository.save(theme);
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Theme with ID " + id + " not found"));
    }

    public void deleteTheme(Long id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + id
                        + " not found"));

        List<Build> buildsWithTheme = buildRepository.findBuildsByThemeId(id);
        if (!buildsWithTheme.isEmpty()) {
            throw new EntityInUseException("Cannot delete theme because it is associated with "
                    +   "builds");
        }

        themeRepository.delete(theme);
    }
}