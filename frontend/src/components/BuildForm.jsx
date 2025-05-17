// File: frontend/src/components/BuildForm.jsx
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { createBuild, updateBuild } from '../api/buildService';

import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';

const AUTHORS_PARAM_NAME = "authors";
const THEMES_PARAM_NAME = "themes";
const COLORS_PARAM_NAME = "colors";
const NAME_PARAM_NAME = "name";
const DESCRIPTION_PARAM_NAME = "description";
const SCHEM_FILE_PARAM_NAME = "schemFile";
const SCREENSHOTS_PARAM_NAME = "screenshots"; // Backend expects an array with this key

const INITIAL_FORM_STATE = {
    name: '',
    authorNames: '',
    themeNames: '',
    description: '',
    colorNames: '',
    screenshotUrls: '', // This state holds the comma-separated string for the TextField
    schemFile: null,
};

const DEFAULT_EMPTY_STRING = '';
const CURRENT_FILE_RETAINED_TEXT = 'Current file retained';
const UPLOAD_SCHEM_TEXT = 'Upload Schematic File (.schem)*';
const REPLACE_SCHEM_TEXT = 'Replace Schematic File (.schem)';

/**
 * Form component for creating or editing a Minecraft build.
 * It handles input fields for build details, file uploads, and submission.
 */
function BuildForm({ onBuildCreated, existingBuild, onBuildUpdated, onCancelForm }) {
    const [formData, setFormData] = useState(INITIAL_FORM_STATE);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(DEFAULT_EMPTY_STRING);
    const [selectedFileName, setSelectedFileName] = useState(DEFAULT_EMPTY_STRING);

    const isEditMode = Boolean(existingBuild?.id);

    useEffect(() => {
        if (existingBuild) {
            const uniqueScreenshotUrls = existingBuild.screenshots &&
            Array.isArray(existingBuild.screenshots)
                ? [...new Set(
                    existingBuild.screenshots.filter(url =>
                        typeof url === 'string' && url.trim() !== DEFAULT_EMPTY_STRING
                    )
                )].join(', ')
                : DEFAULT_EMPTY_STRING;

            setFormData({
                name: existingBuild.name || DEFAULT_EMPTY_STRING,
                authorNames: existingBuild.authors?.map(a => a.name).join(', ') ||
                    DEFAULT_EMPTY_STRING,
                themeNames: existingBuild.themes?.map(t => t.name).join(', ') ||
                    DEFAULT_EMPTY_STRING,
                description: existingBuild.description || DEFAULT_EMPTY_STRING,
                colorNames: existingBuild.colors?.map(c => c.name).join(', ') ||
                    DEFAULT_EMPTY_STRING,
                screenshotUrls: uniqueScreenshotUrls,
                schemFile: null,
            });
            setSelectedFileName(
                existingBuild.schemFile ? CURRENT_FILE_RETAINED_TEXT : DEFAULT_EMPTY_STRING
            );
        } else {
            setFormData(INITIAL_FORM_STATE);
            setSelectedFileName(DEFAULT_EMPTY_STRING);
        }
        setSuccessMessage(DEFAULT_EMPTY_STRING);
        setError(null);
    }, [existingBuild]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prevData => ({ ...prevData, [name]: value }));
    };

    const handleFileChange = (e) => {
        const file = e.target.files?.[0] || null;
        setFormData(prevData => ({ ...prevData, schemFile: file }));

        let fileNameToSet = DEFAULT_EMPTY_STRING;
        if (file) {
            fileNameToSet = file.name;
        } else if (isEditMode && existingBuild?.schemFile) {
            fileNameToSet = CURRENT_FILE_RETAINED_TEXT;
        }
        setSelectedFileName(fileNameToSet);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError(null);
        setSuccessMessage(DEFAULT_EMPTY_STRING);

        const data = new FormData();
        data.append(NAME_PARAM_NAME, formData.name);

        (formData.authorNames || DEFAULT_EMPTY_STRING).split(',').map(s => s.trim()).filter(s => s)
            .forEach(author => data.append(AUTHORS_PARAM_NAME, author));
        (formData.themeNames || DEFAULT_EMPTY_STRING).split(',').map(s => s.trim()).filter(s => s)
            .forEach(theme => data.append(THEMES_PARAM_NAME, theme));
        (formData.colorNames || DEFAULT_EMPTY_STRING).split(',').map(s => s.trim()).filter(s => s)
            .forEach(color => data.append(COLORS_PARAM_NAME, color));

        if (formData.description) {
            data.append(DESCRIPTION_PARAM_NAME, formData.description);
        }

        if (formData.screenshotUrls) {
            (formData.screenshotUrls || DEFAULT_EMPTY_STRING).split(',')
                .map(url => url.trim())
                .filter(url => url)
                .forEach(url => data.append(SCREENSHOTS_PARAM_NAME, url));
        }

        if (!formData.schemFile && !isEditMode) {
            setError("Schematic file is required for new builds.");
            setIsSubmitting(false);
            return;
        }
        if (formData.schemFile) {
            data.append(SCHEM_FILE_PARAM_NAME, formData.schemFile);
        }

        try {
            if (isEditMode && existingBuild?.id) {
                const updated = await updateBuild(existingBuild.id, data);
                setSuccessMessage(`Build "${updated.name}" updated successfully!`);
                if (onBuildUpdated) onBuildUpdated(updated);
            } else {
                const newBuild = await createBuild(data);
                setSuccessMessage(`Build "${newBuild.name}" created successfully!`);
                setFormData(INITIAL_FORM_STATE);
                setSelectedFileName(DEFAULT_EMPTY_STRING);
                if (onBuildCreated) onBuildCreated(newBuild);
            }
        } catch (err) {
            const action = isEditMode ? 'update' : 'create';
            setError(err.message || `Failed to ${action} build.`);
            console.error("Form submit error:", err);
        } finally {
            setIsSubmitting(false);
        }
    };

    const formTitle = isEditMode ?
        `Edit Build: ${existingBuild?.name || '...'}` : 'Create New Build';
    const modeSpecificSubmitText = isEditMode ? 'Update Build' : 'Create Build';
    const submitButtonText = isSubmitting ? 'Submitting...' : modeSpecificSubmitText;
    const cancelButtonText = isEditMode ? 'Cancel Edit' : 'Cancel Create';

    let fileUploadButtonText = UPLOAD_SCHEM_TEXT;
    if (selectedFileName) {
        fileUploadButtonText = selectedFileName;
    } else if (isEditMode && existingBuild?.schemFile) {
        fileUploadButtonText = REPLACE_SCHEM_TEXT;
    }


    return (
        <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            p: { xs: 1, sm: 1.5 },
            bgcolor: 'background.paper',
            borderRadius: 1
        }}>
            <Typography component="h2" variant="h5" sx={{ mb: 1.5 }}>
                {formTitle}
            </Typography>

            {error &&
                <Alert severity="error" sx={{ width: '100%', mb: 1.5 }}>{error}</Alert>}
            {successMessage && !error &&
                <Alert severity="success" sx={{ width: '100%', mb: 1.5 }}>
                    {successMessage}
                </Alert>}

            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 0, width: '100%' }}>
                <TextField margin="dense" required fullWidth id="name" label="Build Name"
                           name="name" value={formData.name} onChange={handleChange} autoFocus
                           disabled={isSubmitting} size="small" />
                <TextField margin="dense" required fullWidth id="authorNames"
                           label="Authors (comma-separated)" name="authorNames"
                           value={formData.authorNames} onChange={handleChange}
                           disabled={isSubmitting} size="small" />
                <TextField margin="dense" required fullWidth id="themeNames"
                           label="Themes (comma-separated)" name="themeNames"
                           value={formData.themeNames} onChange={handleChange}
                           disabled={isSubmitting} size="small" />
                <TextField margin="dense" required fullWidth id="colorNames"
                           label="Colors (comma-separated)" name="colorNames"
                           value={formData.colorNames} onChange={handleChange}
                           disabled={isSubmitting} size="small" />
                <TextField margin="dense" fullWidth id="description" label="Description"
                           name="description" multiline rows={2} value={formData.description}
                           onChange={handleChange} disabled={isSubmitting} size="small" />
                <TextField
                    margin="dense"
                    fullWidth
                    id="screenshotUrls"
                    label="Screenshot URLs (comma-separated)"
                    name="screenshotUrls"
                    value={formData.screenshotUrls}
                    onChange={handleChange}
                    helperText="Enter direct image URLs, separated by commas."
                    disabled={isSubmitting}
                    size="small"
                />

                <Button
                    variant="outlined"
                    component="label"
                    fullWidth
                    startIcon={<CloudUploadIcon />}
                    sx={{ mt: 1.5, mb: 0.5, textTransform: 'none' }}
                    disabled={isSubmitting}
                    size="medium"
                >
                    {fileUploadButtonText}
                    <input type="file" hidden accept=".schem" onChange={handleFileChange}
                           disabled={isSubmitting} />
                </Button>
                {selectedFileName && formData.schemFile && (
                    <Typography variant="caption" display="block"
                                sx={{ textAlign: 'center', mt: 0, mb: 0.5 }}>
                        Selected: {selectedFileName}
                    </Typography>
                )}

                <Box sx={{ display: 'flex', gap: 2, mt: 1.5, mb: 1 }}>
                    <Button
                        type="submit"
                        fullWidth
                        variant="contained"
                        disabled={isSubmitting || !formData.name ||
                            !formData.authorNames || !formData.themeNames ||
                            !formData.colorNames ||
                            (!isEditMode && !formData.schemFile)}
                        startIcon={isSubmitting ?
                            <CircularProgress size={20} color="inherit" /> : null}
                        size="medium"
                    >
                        {submitButtonText}
                    </Button>
                    <Button
                        type="button"
                        fullWidth
                        variant="outlined"
                        onClick={onCancelForm}
                        disabled={isSubmitting}
                        size="medium"
                    >
                        {cancelButtonText}
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}

BuildForm.propTypes = {
    onBuildCreated: PropTypes.func.isRequired,
    existingBuild: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
        name: PropTypes.string,
        authors: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        themes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        description: PropTypes.string,
        colors: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        screenshots: PropTypes.arrayOf(PropTypes.string),
        schemFile: PropTypes.object, // Can be File object or metadata
    }),
    onBuildUpdated: PropTypes.func.isRequired,
    onCancelForm: PropTypes.func.isRequired,
};

BuildForm.defaultProps = {
    existingBuild: null,
};

export default BuildForm;