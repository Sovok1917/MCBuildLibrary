// File: frontend/src/api/themeService.js
const API_BASE_URL = '/api/themes';

/**
 * Fetches all themes from the backend.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of theme DTOs.
 */
export const getAllThemes = async () => {
    // ... (implementation from previous step)
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
    const response = await fetch(`${API_BASE_URL}/${id}?name=${encodeURIComponent(newName)}`, {
        method: 'PUT',
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
    const response = await fetch(`${API_BASE_URL}/${id}`, {
        method: 'DELETE',
    });
    if (!response.ok && response.status !== 204) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to delete theme ${id}`);
    }
};