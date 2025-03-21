package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.dao.BuildDao;
import sovok.mcbuildlibrary.model.Build;

@Service
public class BuildService {

    private final BuildDao buildDao;

    // Constructor-based dependency injection
    public BuildService(BuildDao buildDao) {
        this.buildDao = buildDao;
    }

    public Optional<Build> findBuildById(String id) {
        return buildDao.findById(id);
    }

    public List<Build> findAll() {
        return buildDao.findAll();
    }

    public List<Build> filterBuilds(String author, String name, String theme, List<String> colors) {
        return buildDao.filterBuilds(author, name, theme, colors);
    }
}
