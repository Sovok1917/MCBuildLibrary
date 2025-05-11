// File: frontend/src/api/themeService.js
const API_BASE_URL = '/api/themes'; // Proxied path

/**
 * Fetches all themes from the backend.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of theme DTOs.
 * @throws {Error} If the network response is not ok.
 */
export const getAllThemes = async () => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch themes');
    }
    return response.json();
};