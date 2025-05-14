// File: frontend/src/api/buildService.js

const API_BASE_URL = '/api/builds';

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
 * Fetches builds based on filter criteria. (GET request - no CSRF token needed)
 * @param {Object} filters - An object containing filter criteria.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of filtered build objects.
 */
export const getFilteredBuilds = async ({ name, author, theme, color }) => {
    const queryParams = new URLSearchParams();
    if (name) queryParams.append('name', name);
    if (author) queryParams.append('author', author);
    if (theme) queryParams.append('theme', theme);
    if (color) queryParams.append('color', color);

    const queryString = queryParams.toString();
    const url = queryString ? `${API_BASE_URL}/query?${queryString}` : API_BASE_URL;

    console.log(`Fetching builds from URL: ${url}`);

    const response = await fetch(url);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({
            message: 'Network response was not ok'
        }));
        throw new Error(errorData.detail || errorData.message || 'Failed to fetch filtered builds');
    }
    return response.json();
};

/**
 * Creates a new build.
 * @param {FormData} buildData - The build data as FormData.
 * @returns {Promise<Object>} A promise that resolves to the created build object.
 */
export const createBuild = async (buildData) => {
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {}; // For FormData, Content-Type is set by the browser
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: headers,
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
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {}; // For FormData, Content-Type is set by the browser
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(`${API_BASE_URL}/${identifier}`, {
        method: 'PUT',
        headers: headers,
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
    const csrfToken = getCookie('XSRF-TOKEN');
    const headers = {};
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }

    const response = await fetch(`${API_BASE_URL}/${identifier}`, {
        method: 'DELETE',
        headers: headers,
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

/**
 * Fetches builds related to a specific entity ID. (GET request - no CSRF token needed)
 * @param {string} type - The type of the entity ('author', 'theme', 'color').
 * @param {number|string} id - The ID of the entity.
 * @returns {Promise<Array<Object>>} A promise that resolves to an array of related build objects.
 */
export const getBuildsByRelatedEntity = async (type, id) => {
    if (!type || id == null) {
        throw new Error('Entity type and ID are required for related build search.');
    }
    const queryParams = new URLSearchParams();
    queryParams.append('type', type);
    queryParams.append('id', id.toString());

    const url = `${API_BASE_URL}/related?${queryParams.toString()}`;

    const response = await fetch(url);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Network response was not ok' }));
        throw new Error(errorData.detail || errorData.message || `Failed to fetch builds related to ${type} ID ${id}`);
    }
    return response.json();
};