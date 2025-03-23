// file: src/main/java/sovok/mcbuildlibrary/service/ThemeService.java
package sovok.mcbuildlibrary.service;

import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.ThemeRepository;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class ThemeService {
    private final ThemeRepository themeRepository;

    public ThemeService(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
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
            throw new IllegalArgumentException("Theme with name '" + name + "' already exists");
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
                    if (themeRepository.findByName(newName).isPresent() && !theme.getName().equals(newName)) {
                        throw new IllegalArgumentException("Another theme with name '" + newName + "' already exists");
                    }
                    theme.setName(newName);
                    return themeRepository.save(theme);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + id + " not found"));
    }

    public void deleteTheme(Long id) {
        if (!themeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Theme with ID " + id + " not found");
        }
        themeRepository.deleteById(id);
    }
}