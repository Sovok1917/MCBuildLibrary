package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.dto.ThemeDto;
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

    private ThemeDto convertToDto(Theme theme) {
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo
                = buildRepository.findBuildIdAndNameByThemeId(theme.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new ThemeDto(theme.getId(), theme.getName(), relatedBuildDtos);
    }

    public Theme findOrCreateTheme(String name) {
        Optional<Theme> themeOpt = themeRepository.findByName(name);
        return themeOpt.orElseGet(() -> {
            Theme newTheme = Theme.builder().name(name).build();
            return themeRepository.save(newTheme);
        });
    }

    public Theme createTheme(String name) {
        Optional<Theme> existingTheme = themeRepository.findByName(name);
        if (existingTheme.isPresent()) {
            throw new EntityInUseException("A theme with the name '" + name + "' already exists.");
        }
        Theme theme = Theme.builder().name(name).build();
        return themeRepository.save(theme);
    }

    public Optional<ThemeDto> findThemeDtoById(Long id) {
        return themeRepository.findById(id).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findAllThemeDtos() {
        List<Theme> themes = themeRepository.findAll();
        if (themes.isEmpty()) {
            throw new ResourceNotFoundException("No themes are currently available");
        }
        return themes.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findThemeDtos(String name) {
        List<Theme> themes = themeRepository.fuzzyFindByName(name);
        return themes.stream().map(this::convertToDto).toList();
    }

    public List<Theme> findThemes(String name) {
        if (name != null && !name.trim().isEmpty()) {
            String pattern = "%" + name.toLowerCase() + "%";
            return themeRepository.findByNameLike(pattern);
        } else {
            return themeRepository.findAll();
        }
    }

    public Theme updateTheme(Long id, String newName) {
        return themeRepository.findById(id)
                .map(theme -> {
                    Optional<Theme> themeWithSameName = themeRepository.findByName(newName);
                    if (themeWithSameName.isPresent()
                            && !themeWithSameName.get().getId().equals(id)) {
                        throw new EntityInUseException("A theme with the name '" + newName
                                + "' already exists.");
                    }
                    theme.setName(newName);
                    return themeRepository.save(theme);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + id
                        + " not found"));
    }

    private void deleteThemeInternal(Theme theme) {
        List<Build> buildsWithTheme = buildRepository.findBuildsByThemeId(theme.getId());
        if (!buildsWithTheme.isEmpty()) {
            throw new EntityInUseException("Cannot delete theme '" + theme.getName()
                    + "' because it is associated with " + buildsWithTheme.size() + " build(s).");
        }
        themeRepository.delete(theme);
    }

    public void deleteTheme(Long id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theme with ID " + id
                        + " not found"));
        deleteThemeInternal(theme);
    }

    public void deleteThemeByName(String name) {
        Theme theme = themeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Theme with name '" + name
                        + "' not found"));
        deleteThemeInternal(theme);
    }
}