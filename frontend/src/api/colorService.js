// File: frontend/src/api/colorService.js
const API_BASE_URL = '/api/colors'; // Proxied path

/**
 * Fetches all colors from the backend.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of color DTOs.
 * @throws {Error} If the network response is not ok.
 */
export const getAllColors = async () => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch colors');
    }
    return response.json();
};