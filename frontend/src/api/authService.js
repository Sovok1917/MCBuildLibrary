// File: frontend/src/api/authService.js

const LOGIN_URL = '/login'; // Spring Security's default form login processing URL
const LOGOUT_URL = '/logout'; // Spring Security's default logout URL
const CURRENT_USER_URL = '/api/users/me'; // Backend endpoint for current user

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

    const response = await fetch(LOGIN_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params,
    });

    if (response.ok) {
        // Login POST was successful, session cookie should be set.
        // Now fetch the user details. If getCurrentUser() throws, it will propagate.
        const user = await getCurrentUser();
        if (!user) {
            // This case means getCurrentUser() returned null after a successful login POST,
            // which implies an issue fetching user details or an invalid session despite 200 OK.
            console.error(
                "Login POST successful, but getCurrentUser() returned null or failed silently.");
            throw new Error('Login succeeded, but failed to retrieve valid user data.');
        }
        return user; // Successfully logged in and fetched user details
    } else {
        // Handle non-ok responses from the /login POST itself
        let errorMessage = `Login failed. Status: ${response.status}`;
        // Use optional chaining for response.url
        if (response.url?.includes("/login?error")) {
            errorMessage = 'Invalid username or password.';
        } else {
            try {
                const errorBody = await response.json();
                errorMessage = errorBody.message || errorBody.detail || errorMessage;
            } catch (e) {
                // Ignore if response body is not JSON or empty
            }
        }
        console.error('Login failed:', errorMessage, 'URL:', response.url);
        throw new Error(errorMessage);
    }
};

/**
 * Logs out the current user.
 * @returns {Promise<void>}
 */
export const logout = async () => {
    const response = await fetch(LOGOUT_URL, {
        method: 'POST',
    });

    if (!response.ok) {
        console.warn('Logout API call failed. Status:', response.status);
        // Client-side state is cleared by AuthContext regardless.
        // No need to throw an error here unless server confirmation is critical.
    }
};

/**
 * Fetches the currently authenticated user's details.
 * @returns {Promise<Object|null>} User object or null if not authenticated or on error.
 */
export const getCurrentUser = async () => {
    try {
        const response = await fetch(CURRENT_USER_URL);

        if (response.status === 401) {
            return null; // Not authenticated
        }

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const message = errorData.message || errorData.detail ||
                `Failed to fetch current user. Status: ${response.status}`;
            console.error("Error fetching current user:", message);
            // For a general "get current user" check, returning null on error is often safer
            // than throwing and potentially breaking UI that just wants to know "is someone logged in?"
            return null;
        }

        const user = await response.json();

        if (user) {
            // Ensure user.roles is always an array, even if authorities is missing.
            user.roles = user.authorities?.map(auth =>
                (typeof auth === 'string' ? auth : auth?.authority)
            ) || [];
        }
        return user;

    } catch (error) { // Network errors or if response.json() fails
        console.debug('Exception during getCurrentUser API call:', error.message);
        return null; // Assume not authenticated on any exception during this check
    }
};