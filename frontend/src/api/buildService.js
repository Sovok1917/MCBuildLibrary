// File: frontend/src/api/buildService.js

const API_BASE_URL = '/api/builds';

/**
 * Fetches all builds from the backend.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of build objects.
 * @throws {Error} If the network response is not ok.
 */
export const getAllBuilds = async () => {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch builds');
    }
    return response.json();
};

/**
 * Fetches builds based on filter criteria.
 * @param {Object} filters - An object containing filter criteria.
 * @param {string} [filters.author] - Author name to filter by.
 * @param {string} [filters.theme] - Theme name to filter by.
 * @param {string} [filters.color] - Color name to filter by.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of filtered build objects.
 */
export const getFilteredBuilds = async ({ author, theme, color }) => {
    const queryParams = new URLSearchParams();
    if (author) queryParams.append('author', author);
    if (theme) queryParams.append('theme', theme);
    if (color) queryParams.append('color', color);

    const queryString = queryParams.toString();
    const url = queryString ? `${API_BASE_URL}/query?${queryString}` : API_BASE_URL;

    const response = await fetch(url);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch filtered builds');
    }
    return response.json();
};


/**
 * Fetches a single build by its identifier (ID or name).
 * @param {string|number} identifier The ID or name of the build.
 * @returns {Promise<Object>} A promise that resolves to the build object.
 * @throws {Error} If the network response is not ok.
 */
export const getBuildByIdentifier = async (identifier) => {
    // ... (implementation from previous steps)
    const response = await fetch(`${API_BASE_URL}/${identifier}`);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to fetch build ${identifier}`);
    }
    return response.json();
};

/**
 * Creates a new build.
 * @param {FormData} buildData - The build data as FormData.
 * @returns {Promise<Object>} A promise that resolves to the created build object.
 */
export const createBuild = async (buildData) => {
    // ... (implementation from previous steps)
    const response = await fetch(API_BASE_URL, {
        method: 'POST',
        body: buildData,
    });
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        let errorMessage = 'Failed to create build.';
        if (errorData.detail) { errorMessage = errorData.detail; }
        else if (errorData.message && errorData.details) { errorMessage = `${errorData.message}: ${JSON.stringify(errorData.details)}`; }
        else if (errorData.message) { errorMessage = errorData.message; }
        throw new Error(errorMessage);
    }
    return response.json();
};

/**
 * Updates an existing build.
 * @param {string|number} identifier The ID or name of the build to update.
 * @param {FormData} buildData - The updated build data as FormData.
 * @returns {Promise<Object>} A promise that resolves to the updated build object.
 */
export const updateBuild = async (identifier, buildData) => {
    // ... (implementation from previous steps)
    const response = await fetch(`${API_BASE_URL}/${identifier}`, {
        method: 'PUT',
        body: buildData,
    });
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        let errorMessage = `Failed to update build ${identifier}.`;
        if (errorData.detail) { errorMessage = errorData.detail; }
        else if (errorData.message && errorData.details) { errorMessage = `${errorData.message}: ${JSON.stringify(errorData.details)}`; }
        else if (errorData.message) { errorMessage = errorData.message; }
        throw new Error(errorMessage);
    }
    return response.json();
};

/**
 * Deletes a build by its identifier.
 * @param {string|number} identifier The ID or name of the build to delete.
 * @returns {Promise<void>} A promise that resolves when the build is deleted.
 */
export const deleteBuild = async (identifier) => {
    // ... (implementation from previous steps)
    const response = await fetch(`${API_BASE_URL}/${identifier}`, {
        method: 'DELETE',
    });
    if (!response.ok) {
        let errorMessage = `Failed to delete build ${identifier}`;
        try {
            const errorData = await response.json();
            errorMessage = errorData.detail || errorData.message || errorMessage;
        } catch (e) { /* Ignore */ }
        throw new Error(errorMessage);
    }
};