package sovok.mcbuildlibrary.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable; // Import PathVariable
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to forward non-API, non-static resource requests to the SPA's index.html.
 * This allows client-side routing to take over for paths like /login, /register, etc.
 * It excludes common backend prefixes and paths with file extensions.
 */
@Controller
public class SpaController {
    
    private static final Logger logger = LoggerFactory.getLogger(SpaController.class);
    
    // Regex for common backend/static prefixes to exclude from SPA forwarding.
    // Breaking this down for readability and to avoid long lines.
    private static final String EXCLUDED_PREFIXES_PART_1 = "api|static|assets|swagger-ui";
    private static final String EXCLUDED_PREFIXES_PART_2 = "v3/api-docs|_error|actuator";
    private static final String EXCLUDED_PREFIXES_PART_3 = "images|css|js";
    private static final String EXCLUDED_PREFIXES_REGEX =
            EXCLUDED_PREFIXES_PART_1 + "|"
                    + EXCLUDED_PREFIXES_PART_2 + "|"
                    + EXCLUDED_PREFIXES_PART_3;
    
    // Regex for a single path variable segment:
    // - Must not start with any of the excluded prefixes.
    // - Must not contain a dot (i.e., no file extension).
    private static final String SINGLE_SPA_PATH_VARIABLE_REGEX =
            "^(?!(" + EXCLUDED_PREFIXES_REGEX + "))[^.]*$";
    
    /**
     * Forwards requests to /index.html for paths that are likely client-side routes.
     * This includes the root, single-segment paths (e.g., /login), and multi-segment
     * paths (e.g., /user/settings) that don't match excluded prefixes or file patterns.
     *
     * @param request The incoming HTTP servlet request.
     * @param ignoredPath    Optional path variable captured from the URL. It's required by Spring
     *                MVC if defined in the pattern but may not be used if the primary goal
     *                is just to match the route for forwarding.
     * @return The forward instruction to "/index.html".
     */
    @RequestMapping(
            value = {"/", // Root path
                     // "/{path:" + SINGLE_SPA_PATH_VARIABLE_REGEX + "}", // Single segment SPA path
                     "/{path:" + SINGLE_SPA_PATH_VARIABLE_REGEX + "}/**" // Multi-segment SPA path
            }
    )
    public String forward(
            HttpServletRequest request,
            @PathVariable(name = "path", required = false) String ignoredPath) {
        // The 'path' variable is captured to satisfy the @RequestMapping definition.
        // It's not strictly needed for the forwarding logic itself here.
        logger.debug("SPA_FORWARDER: Forwarding request for URI \"{}\" to /index.html",
                request.getRequestURI());
        return "forward:/index.html";
    }
}