package sovok.mcbuildlibrary.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

/**
 * Custom AuthenticationSuccessHandler that simply returns a 200 OK status
 * on successful login, instead of redirecting. This is suitable for SPAs
 * where the client handles routing after login.
 */
@Component("restAuthenticationSuccessHandler")
public class RestAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // RequestCache is used by Spring Security to save the original request
    // that was interrupted by the authentication process. We don't need to redirect to it.
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        // Clear any previously saved request, as we are not redirecting to it.
        // This prevents Spring Security from trying to redirect to a "saved request"
        // from the backend's perspective.
        requestCache.removeRequest(request, response);

        // Just send a 200 OK. The client (React app) will then typically
        // fetch user details or navigate as needed.
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().flush(); // Ensure the response is committed
    }
}