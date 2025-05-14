// File: frontend/src/api/authService.js

const LOGIN_URL = '/login'; // Spring Security's default form login processing URL
const LOGOUT_URL = '/logout'; // Spring Security's default logout URL
const CURRENT_USER_URL = '/api/users/me'; // Backend endpoint for current user
const REGISTER_URL = '/api/users/register'; // Backend endpoint for registration

/**
 * Helper function to get a cookie by name.
 * @param {string} name The name of the cookie.
 * @returns {string|null} The cookie value or null if not found.
 */
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
        const cookieValue = parts.pop().split(';').shift();
        // Decode if necessary, though Spring's default XSRF-TOKEN is usually not URL encoded
        return cookieValue ? decodeURIComponent(cookieValue) : null;
    }
    return null;
}

/**
 * Attempts to log in the user with the provided credentials.
 * @param {string} username The username.
 * @param {string} password The password.
 * @returns {Promise<Object>} User object if login is successful.
 * @throws {Error} If login fails.
 */
export const login = async (username, password) => {
    const params = new URLSearchParams();
    params.append('username', username);
    params.append('password', password);

    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
    };
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(LOGIN_URL, {
        method: 'POST',
        headers: headers, // Pass updated headers
        body: params,
    });

    if (response.ok) {
        // Login POST was successful, session cookie should be set.
        // Now fetch the user details. If getCurrentUser() throws, it will propagate.
        const user = await getCurrentUser();
        if (!user) {
            console.error(
                "Login POST successful, but getCurrentUser() returned null or failed silently.");
            throw new Error('Login succeeded, but failed to retrieve valid user data.');
        }
        return user; // Successfully logged in and fetched user details
    } else {
        // Handle non-ok responses from the /login POST itself
        let errorMessage = `Login failed. Status: ${response.status}`;
        // Check if Spring Security redirected to /login?error (common for bad credentials)
        // Spring Security's default behavior for form login failure is a redirect.
        // The actual response status might be 200 OK if it's serving the login page with an error.
        // However, for SPA, a 401 from the POST itself is more direct.
        // If response.url includes '/login?error', it's a strong indicator of bad credentials
        // from Spring Security's perspective.
        if (response.url?.includes("/login?error")) {
            errorMessage = 'Invalid username or password.';
        } else if (response.status === 401 || response.status === 403) {
            // Handle explicit 401 (Unauthorized) or 403 (Forbidden - often CSRF)
            errorMessage = 'Authentication failed. Please check credentials or try again.';
            try {
                // Attempt to get a more specific message if the server sent one
                const errorBody = await response.json();
                errorMessage = errorBody.message || errorBody.detail || errorMessage;
            } catch (e) {
                // If body is not JSON or empty, use the generic message
            }
        } else {
            // For other non-ok statuses, try to parse an error message
            try {
                const errorBody = await response.json();
                errorMessage = errorBody.message || errorBody.detail || errorMessage;
            } catch (e) {
                // Ignore if response body is not JSON or empty
            }
        }
        console.error('Login failed:', errorMessage, 'URL:', response.url, 'Status:', response.status);
        throw new Error(errorMessage);
    }
};

/**
 * Logs out the current user.
 * @returns {Promise<void>}
 */
export const logout = async () => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {}; // Content-Type not strictly needed for an empty body POST
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(LOGOUT_URL, {
        method: 'POST',
        headers: headers,
    });

    if (!response.ok) {
        console.warn('Logout API call failed. Status:', response.status);
        // Client-side state is cleared by AuthContext regardless.
    }
};

/**
 * Fetches the currently authenticated user's details.
 * GET requests typically do not require CSRF tokens.
 * @returns {Promise<Object|null>} User object or null if not authenticated or on error.
 */
export const getCurrentUser = async () => {
    try {
        const response = await fetch(CURRENT_USER_URL); // GET request

        if (response.status === 401) {
            return null; // Not authenticated
        }

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({})); // Safely parse JSON
            const message = errorData.message || errorData.detail ||
                `Failed to fetch current user. Status: ${response.status}`;
            console.error("Error fetching current user:", message);
            return null;
        }

        const user = await response.json();

        if (user) {
            // Ensure user.roles is always an array
            user.roles = user.authorities?.map(auth =>
                (typeof auth === 'string' ? auth : auth?.authority)
            ) || [];
        }
        return user;

    } catch (error) { // Catches network errors or if response.json() fails
        // Log as debug because this can happen normally if offline
        console.debug('Exception during getCurrentUser API call:', error.message);
        return null; // Assume not authenticated on any exception
    }
};

/**
 * Attempts to register a new user.
 * @param {string} username The desired username.
 * @param {string} password The desired password.
 * @returns {Promise<Object>} User info object if registration is successful.
 * @throws {Error} If registration fails (e.g., username taken, validation error).
 */
export const register = async (username, password) => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json',
    };
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(REGISTER_URL, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({ username, password }),
    });

    if (response.ok) { // HTTP 201 Created
        return await response.json(); // Return the registered user info (username, roles)
    } else {
        // Handle registration errors
        let errorMessage = `Registration failed. Status: ${response.status}`;
        try {
            const errorBody = await response.json();
            // Handle specific Spring validation error structure
            if (errorBody.details && typeof errorBody.details === 'object') {
                errorMessage = Object.entries(errorBody.details)
                    .map(([field, message]) => `${field}: ${message}`)
                    .join('; ');
            } else { // General error structure from ProblemDetail
                errorMessage = errorBody.detail || errorBody.message || errorMessage;
            }
        } catch (e) {
            // Ignore if response body is not JSON or empty
        }
        console.error('Registration failed:', errorMessage);
        throw new Error(errorMessage);
    }
};