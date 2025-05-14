// File: frontend/src/api/colorService.js
const API_BASE_URL = '/api/colors';

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
 * Fetches all colors from the backend. (GET request - no CSRF token needed)
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of color DTOs.
 */
export const getAllColors = async () => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch colors');
    }
    return response.json();
};

/**
 * Updates a color's name.
 * @param {number|string} id The ID of the color to update.
 * @param {string} newName The new name for the color.
 * @returns {Promise<Object>} A promise that resolves to the updated color object.
 */
export const updateColor = async (id, newName) => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {
        'Content-Type': 'application/json', // Assuming the backend expects this for PUT if body was present
    };
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(`${API_BASE_URL}/${id}?name=${encodeURIComponent(newName)}`, {
        method: 'PUT',
        headers: headers,
        // No body for this specific PUT, but headers are good practice
    });
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to update color ${id}`);
    }
    return response.json();
};

/**
 * Deletes a color by its ID.
 * @param {number|string} id The ID of the color to delete.
 * @returns {Promise<void>} A promise that resolves when the color is deleted.
 */
export const deleteColor = async (id) => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {};
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(`${API_BASE_URL}/${id}`, {
        method: 'DELETE',
        headers: headers,
    });
    if (!response.ok && response.status !== 204) { // 204 No Content is a success for DELETE
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to delete color ${id}`);
    }
    // No content expected on successful DELETE (204)
};