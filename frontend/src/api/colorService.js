// File: frontend/src/api/colorService.js
const API_BASE_URL = '/api/colors';

/**
 * Fetches all colors from the backend.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of color DTOs.
 */
export const getAllColors = async () => {
    // ... (implementation from previous step)
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
    const response = await fetch(`${API_BASE_URL}/${id}?name=${encodeURIComponent(newName)}`, {
        method: 'PUT',
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
    const response = await fetch(`${API_BASE_URL}/${id}`, {
        method: 'DELETE',
    });
    if (!response.ok && response.status !== 204) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to delete color ${id}`);
    }
};