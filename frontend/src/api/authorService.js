// File: frontend/src/api/authorService.js
const API_BASE_URL = '/api/authors';

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
 * Fetches all authors from the backend. (GET request - no CSRF token needed)
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of author DTOs.
 */
export const getAllAuthors = async () => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch authors');
    }
    return response.json();
};

/**
 * Updates an author's name.
 * @param {number|string} id The ID of the author to update.
 * @param {string} newName The new name for the author.
 * @returns {Promise<Object>} A promise that resolves to the updated author object.
 */
export const updateAuthor = async (id, newName) => {
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
        throw new Error(errorData.detail || errorData.message || `Failed to update author ${id}`);
    }
    return response.json();
};

/**
 * Deletes an author by their ID.
 * @param {number|string} id The ID of the author to delete.
 * @returns {Promise<void>} A promise that resolves when the author is deleted.
 */
export const deleteAuthor = async (id) => {
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
        throw new Error(errorData.detail || errorData.message || `Failed to delete author ${id}`);
    }
};