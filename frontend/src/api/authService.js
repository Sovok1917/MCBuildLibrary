// File: frontend/src/api/authService.js

const LOGIN_URL = '/api/perform_login'; // UPDATED - Spring Security's form login processing URL
const LOGOUT_URL = '/api/perform_logout'; // UPDATED - Spring Security's default logout URL
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

    const response = await fetch(LOGIN_URL, { // Uses updated LOGIN_URL
        method: 'POST',
        headers: headers,
        body: params,
    });

    if (response.ok) {
        const user = await getCurrentUser();
        if (!user) {
            console.error(
                "Login POST successful, but getCurrentUser() returned null or failed silently.");
            throw new Error('Login succeeded, but failed to retrieve valid user data.');
        }
        return user;
    } else {
        let errorMessage = `Login failed. Status: ${response.status}`;
        // Check if Spring Security redirected to /login?error (common for bad credentials)
        // This check might need adjustment if loginPage is not explicitly /login anymore
        // For API-based login, a 401 from the POST itself is more direct.
        if (response.url?.includes("?error")) { // Generic error check
            errorMessage = 'Invalid username or password.';
        } else if (response.status === 401 || response.status === 403) {
            errorMessage = 'Authentication failed. Please check credentials or try again.';
            try {
                const errorBody = await response.json();
                errorMessage = errorBody.message || errorBody.detail || errorMessage;
            } catch (e) {
                // If body is not JSON or empty, use the generic message
            }
        } else {
            try {
                const errorBody = await response.json();
                errorMessage = errorBody.message || errorBody.detail || errorMessage;
            } catch (e) {
                // Ignore if response body is not JSON or empty
            }
        }
        console.error('Login failed:', errorMessage, 'URL:', response.url, 'Status:',
            response.status);
        throw new Error(errorMessage);
    }
};

/**
 * Logs out the current user.
 * @returns {Promise<void>}
 */
export const logout = async () => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {};
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(LOGOUT_URL, { // Uses updated LOGOUT_URL
        method: 'POST',
        headers: headers,
    });

    if (!response.ok) {
        console.warn('Logout API call failed. Status:', response.status);
    }
};

// getCurrentUser and register functions remain unchanged
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