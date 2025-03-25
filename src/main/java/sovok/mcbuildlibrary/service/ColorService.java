package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;

@Service
public class ColorService {
    private final ColorRepository colorRepository;
    private final BuildRepository buildRepository; // Add BuildRepository

    public ColorService(ColorRepository colorRepository, BuildRepository buildRepository) {
        this.colorRepository = colorRepository;
        this.buildRepository = buildRepository;
    }

    public Color findOrCreateColor(String name) {
        Optional<Color> colorOpt = colorRepository.findByName(name);
        return colorOpt.orElseGet(() -> {
            Color newColor = Color.builder().name(name).build();
            return colorRepository.save(newColor);
        });
    }

    public Color createColor(String name) {
        if (colorRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException(
                    "Color with name '" + name + "' already exists");
        }
        Color color = Color.builder().name(name).build();
        return colorRepository.save(color);
    }

    public Optional<Color> findColorById(Long id) {
        return colorRepository.findById(id);
    }

    public List<Color> findAllColors() {
        List<Color> colors = colorRepository.findAll();
        if (colors.isEmpty()) {
            throw new ResourceNotFoundException("No colors are currently available");
        }
        return colors;
    }

    public Color updateColor(Long id, String newName) {
        return colorRepository.findById(id)
                .map(color -> {
                    if (colorRepository.findByName(newName).isPresent()
                            && !color.getName().equals(newName)) {
                        throw new IllegalArgumentException(
                                "Another color with name '" + newName + "' already exists");
                    }
                    color.setName(newName);
                    return colorRepository.save(color);
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Color with ID " + id + " not found"));
    }

    // Add this private method to encapsulate the deletion logic
    private void deleteColorInternal(Color color) {
        List<Build> buildsWithColor = buildRepository.findBuildsByColorId(color.getId());
        if (!buildsWithColor.isEmpty()) {
            throw new EntityInUseException("Cannot delete color because it is associated with "
                    + "builds");
        }
        colorRepository.delete(color);
    }

    // Update the existing deleteColor method
    public void deleteColor(Long id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Color with ID " + id + " not "
                        + "found"));
        deleteColorInternal(color);
    }

    // Add the new deleteColorByName method
    public void deleteColorByName(String name) {
        Color color = colorRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Color with name '" + name
                        + "' not found"));
        deleteColorInternal(color);
    }

    public List<Color> findColors(String name) {
        if (name != null) {
            return colorRepository.findByName(name).map(List::of).orElse(List.of());
        } else {
            return colorRepository.findAll();
        }
    }
}