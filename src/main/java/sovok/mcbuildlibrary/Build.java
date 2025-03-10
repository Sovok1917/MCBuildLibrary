package sovok.mcbuildlibrary;

import java.util.List;

public class Build {
    private String id; // Unique identifier
    private String name; // Name of the build
    private String author; // Creator of the build
    private String theme; // Theme or style of the build
    private String description; // Detailed description of the build
    private List<String> colors; // List of prominent colors used in the build
    private List<String> screenshots; // Paths/URLs to screenshots (if any)
    private String schemFile; // Path/URL to the .schem file

    // Constructor
    @SuppressWarnings("java:S107") // Suppress warnings about too many parameters (temporary)
    public Build(String id, String name, String author, String theme, String description,
                 List<String> colors, List<String> screenshots, String schemFile) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.theme = theme;
        this.description = description;
        this.colors = colors;
        this.screenshots = screenshots;
        this.schemFile = schemFile;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public List<String> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<String> screenshots) {
        this.screenshots = screenshots;
    }

    public String getSchemFile() {
        return schemFile;
    }

    public void setSchemFile(String schemFile) {
        this.schemFile = schemFile;
    }

    @Override
    public String toString() {
        return "Build{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", author='" + author + '\''
                + ", theme='" + theme + '\''
                + ", description='" + description + '\''
                + ", colors=" + colors
                + ", screenshots=" + screenshots
                + ", schemFile='" + schemFile + '\''
                + '}';
    }
}
