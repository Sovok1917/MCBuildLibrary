// File: frontend/src/api/authorService.js
const API_BASE_URL = '/api/authors'; // Proxied path

/**
 * Fetches all authors from the backend.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of author DTOs.
 * @throws {Error} If the network response is not ok.
 */
export const getAllAuthors = async () => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch authors');
    }
    return response.json();
};