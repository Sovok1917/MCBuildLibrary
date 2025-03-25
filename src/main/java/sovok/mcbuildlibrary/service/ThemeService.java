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

    // Add this private method to encapsulate the deletion logic
    private void deleteThemeInternal(Theme theme) {
        List<Build> buildsWithTheme = buildRepository.findBuildsByThemeId(theme.getId());
        if (!buildsWithTheme.isEmpty()) {
            throw new EntityInUseException("Cannot delete theme because it is associated with"
                    + " builds");
        }
        themeRepository.delete(theme);
    }

    // Update the existing deleteTheme method
    public void deleteTheme(Long id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + id
                        + " not found"));
        deleteThemeInternal(theme);
    }

    // Add the new deleteThemeByName method
    public void deleteThemeByName(String name) {
        Theme theme = themeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Theme with name '" + name
                        + "' not found"));
        deleteThemeInternal(theme);
    }

    public List<Theme> findThemes(String name) {
        if (name != null) {
            return themeRepository.findByName(name).map(List::of).orElse(List.of());
        } else {
            return themeRepository.findAll();
        }
    }
}