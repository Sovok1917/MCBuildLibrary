// File: frontend/src/api/authorService.js
const API_BASE_URL = '/api/authors';

/**
 * Fetches all authors from the backend.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of author DTOs.
 */
export const getAllAuthors = async () => {
    // ... (implementation from previous step)
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
    const response = await fetch(`${API_BASE_URL}/${id}?name=${encodeURIComponent(newName)}`, {
        method: 'PUT',
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
    const response = await fetch(`${API_BASE_URL}/${id}`, {
        method: 'DELETE',
    });
    if (!response.ok && response.status !== 204) { // 204 No Content is a success for DELETE
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to delete author ${id}`);
    }
    // No content expected on successful DELETE (204)
};