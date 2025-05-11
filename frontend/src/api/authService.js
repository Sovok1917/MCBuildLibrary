// File: frontend/src/api/authService.js

const LOGIN_URL = '/login'; // Spring Security's default form login processing URL
const LOGOUT_URL = '/logout'; // Spring Security's default logout URL
const CURRENT_USER_URL = '/api/users/me'; // A new backend endpoint we'll need

/**
 * Attempts to log in the user with the provided credentials.
 * Spring Security's formLogin handles this as a POST with x-www-form-urlencoded.
 * @param {string} username
 * @param {string} password
 * @returns {Promise<Object>} User object if login is successful
 * @throws {Error} If login fails
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
        // redirect: 'manual' // Important: Spring Security might redirect on success/failure.
        // 'manual' prevents fetch from automatically following,
        // allowing us to inspect the response.
        // A 200 OK typically means success for form login if no redirect.
        // A redirect (302) might also indicate success (to target) or failure (back to login page).
    });

    // For form login, a successful login usually results in a 200 OK (if no explicit successForwardUrl)
    // or a redirect (302) to the original target or default success page.
    // A failure often results in a redirect back to the login page (e.g., /login?error).
    // We need to infer success if we don't get an error status and a session is likely established.
    if (response.ok || response.status === 200 ) { // 200 OK means login was processed.
        // After successful form login, the session cookie (JSESSIONID) is set by the browser.
        // We should then fetch the current user details.
        try {
            return await getCurrentUser();
        } catch (userError) {
            // This can happen if getCurrentUser fails even after a 200 from /login
            // (e.g., /api/users/me is not yet set up or has issues)
            console.error("Login seemed successful, but failed to fetch user details:", userError);
            throw new Error('Login succeeded but failed to retrieve user data.');
        }
    } else {
        // Attempt to parse error if backend sends a JSON response for errors,
        // though default Spring Security form login usually redirects with ?error query param.
        // For this example, we'll throw a generic error.
        // A more robust solution would inspect response.url after redirect if response.type === 'opaqueredirect'
        console.error('Login failed. Status:', response.status, 'URL:', response.url);
        throw new Error('Login failed. Invalid username or password.');
    }
};

/**
 * Logs out the current user.
 * Spring Security's default logout is often a GET or POST to /logout.
 * @returns {Promise<void>}
 * @throws {Error} If logout fails
 */
export const logout = async () => {
    const response = await fetch(LOGOUT_URL, {
        method: 'POST', // Or GET, depending on your Spring Security config for logout
        // No body needed for default logout usually
    });

    if (!response.ok) {
        // Even if logout API call fails, we often want to clear client-side session.
        // The AuthContext handles clearing client-side user state.
        console.warn('Logout API call failed. Status:', response.status);
        // Depending on strictness, you might throw or just log.
        // Forcing client-side logout is usually safe.
        // throw new Error('Logout failed on the server.');
    }
    // No specific data to return on successful logout
};

/**
 * Fetches the currently authenticated user's details.
 * Requires a backend endpoint like /api/users/me.
 * @returns {Promise<Object|null>} User object or null if not authenticated.
 */
export const getCurrentUser = async () => {
    try {
        const response = await fetch(CURRENT_USER_URL);
        if (response.status === 401 || response.status === 403) { // Unauthorized or Forbidden
            return null; // No user logged in or session expired
        }
        if (!response.ok) {
            // Attempt to get error details if possible
            const errorData = await response.json().catch(() => ({ message: 'Failed to fetch current user' }));
            throw new Error(errorData.message || `Server error: ${response.status}`);
        }
        const user = await response.json();
        // Ensure roles are in a consistent format if needed by hasRole
        if (user && user.authorities) { // Spring Security often uses 'authorities'
            user.roles = user.authorities.map(auth => auth.authority);
        }
        return user;
    } catch (error) {
        // console.debug('Error fetching current user:', error.message);
        // Don't throw an error that breaks the app if just checking session, return null.
        // Throw if it's an unexpected server error during an explicit check.
        if (error.message.startsWith('Server error:')) throw error;
        return null;
    }
};