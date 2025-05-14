// File: frontend/src/api/themeService.js
const API_BASE_URL = '/api/themes';

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
        return cookieValue ? decodeURIComponent(cookieValue) : null;
    }
    return null;
}

/**
 * Fetches all themes from the backend. (GET request - no CSRF token needed)
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of theme DTOs.
 */
export const getAllThemes = async () => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch themes');
    }
    return response.json();
};

/**
 * Updates a theme's name.
 * @param {number|string} id The ID of the theme to update.
 * @param {string} newName The new name for the theme.
 * @returns {Promise<Object>} A promise that resolves to the updated theme object.
 */
export const updateTheme = async (id, newName) => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json',
    };
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(`${API_BASE_URL}/${id}?name=${encodeURIComponent(newName)}`, {
        method: 'PUT',
        headers: headers,
    });
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to update theme ${id}`);
    }
    return response.json();
};

/**
 * Deletes a theme by its ID.
 * @param {number|string} id The ID of the theme to delete.
 * @returns {Promise<void>} A promise that resolves when the theme is deleted.
 */
export const deleteTheme = async (id) => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {};
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(`${API_BASE_URL}/${id}`, {
        method: 'DELETE',
        headers: headers,
    });
    if (!response.ok && response.status !== 204) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to delete theme ${id}`);
    }
};