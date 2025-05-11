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

const AUTHORS_PARAM_NAME = "authors";
const THEMES_PARAM_NAME = "themes";
const COLORS_PARAM_NAME = "colors";
const NAME_PARAM_NAME = "name";
const DESCRIPTION_PARAM_NAME = "description";
const SCHEM_FILE_PARAM_NAME = "schemFile";
const SCREENSHOTS_PARAM_NAME = "screenshots";

const INITIAL_FORM_STATE = {
    name: '',
    authorNames: '',
    themeNames: '',
    description: '',
    colorNames: '',
    screenshotUrls: '',
    schemFile: null,
};

function BuildForm({ onBuildCreated, existingBuild, onBuildUpdated, onCancelForm }) {
    const [formData, setFormData] = useState(INITIAL_FORM_STATE);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState('');
    const [selectedFileName, setSelectedFileName] = useState('');

    const isEditMode = Boolean(existingBuild?.id);

    useEffect(() => {
        if (existingBuild) {
            setFormData({
                name: existingBuild.name || '',
                authorNames: (existingBuild.authors?.map(a => a.name).join(', ') || existingBuild.authorNames) || '',
                themeNames: (existingBuild.themes?.map(t => t.name).join(', ') || existingBuild.themeNames) || '',
                description: existingBuild.description || '',
                colorNames: (existingBuild.colors?.map(c => c.name).join(', ') || existingBuild.colorNames) || '',
                screenshotUrls: (existingBuild.screenshots?.join(', ') || existingBuild.screenshotUrls) || '',
                schemFile: null,
            });
        } else {
            setFormData(INITIAL_FORM_STATE);
        }
        setSelectedFileName('');
        setSuccessMessage('');
        setError(null);
    }, [existingBuild]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prevData => ({ ...prevData, [name]: value }));
    };

    const handleFileChange = (e) => {
        const file = e.target.files?.[0] || null;
        setFormData(prevData => ({ ...prevData, schemFile: file }));
        setSelectedFileName(file ? file.name : '');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError(null);
        setSuccessMessage('');

        const data = new FormData();
        data.append(NAME_PARAM_NAME, formData.name);

        (formData.authorNames || '').split(',').map(s => s.trim()).filter(s => s)
            .forEach(author => data.append(AUTHORS_PARAM_NAME, author));
        (formData.themeNames || '').split(',').map(s => s.trim()).filter(s => s)
            .forEach(theme => data.append(THEMES_PARAM_NAME, theme));
        (formData.colorNames || '').split(',').map(s => s.trim()).filter(s => s)
            .forEach(color => data.append(COLORS_PARAM_NAME, color));

        if (formData.description) {
            data.append(DESCRIPTION_PARAM_NAME, formData.description);
        }
        if (formData.screenshotUrls) {
            (formData.screenshotUrls || '').split(',').map(url => url.trim()).filter(url => url)
                .forEach(url => data.append(SCREENSHOTS_PARAM_NAME, url));
        }

        if (!formData.schemFile && !isEditMode && !existingBuild?.id) {
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
                setSelectedFileName('');
                if (onBuildCreated) onBuildCreated(newBuild);
            }
        } catch (err) {
            setError(err.message || `Failed to ${isEditMode ? 'update' : 'create'} build.`);
            console.error("Form submit error:", err);
        } finally {
            setIsSubmitting(false);
        }
    };

    const formTitle = isEditMode ? `Edit Build: ${existingBuild?.name || '...'}` : 'Create New Build';

    // Explicitly extract nested ternary for submitButtonText
    const modeSpecificSubmitText = isEditMode ? 'Update Build' : 'Create Build';
    const submitButtonText = isSubmitting ? 'Submitting...' : modeSpecificSubmitText;

    const cancelButtonText = isEditMode ? 'Cancel Edit' : 'Cancel Create';

    // Explicitly extract nested ternary for fileUploadText
    const fileUploadTextSuffix = (!isEditMode && !existingBuild?.id) ? '*' : '(Optional)';
    const fileUploadText = selectedFileName || `Upload Schematic File (.schem) ${fileUploadTextSuffix}`;

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', p: { xs: 2, sm: 3 }, bgcolor: 'background.paper', borderRadius: 1 }}>
            <Typography component="h2" variant="h5" gutterBottom>
                {formTitle}
            </Typography>

            {error && <Alert severity="error" sx={{ width: '100%', mb: 2 }}>{error}</Alert>}
            {successMessage && !error && <Alert severity="success" sx={{ width: '100%', mb: 2 }}>{successMessage}</Alert>}

            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
                <TextField margin="normal" required fullWidth id="name" label="Build Name" name="name" value={formData.name} onChange={handleChange} autoFocus disabled={isSubmitting} />
                <TextField margin="normal" required fullWidth id="authorNames" label="Authors (comma-separated)" name="authorNames" value={formData.authorNames} onChange={handleChange} disabled={isSubmitting} />
                <TextField margin="normal" required fullWidth id="themeNames" label="Themes (comma-separated)" name="themeNames" value={formData.themeNames} onChange={handleChange} disabled={isSubmitting} />
                <TextField margin="normal" required fullWidth id="colorNames" label="Colors (comma-separated)" name="colorNames" value={formData.colorNames} onChange={handleChange} disabled={isSubmitting} />
                <TextField margin="normal" fullWidth id="description" label="Description" name="description" multiline rows={3} value={formData.description} onChange={handleChange} disabled={isSubmitting} />
                <TextField margin="normal" fullWidth id="screenshotUrls" label="Screenshot URLs (comma-separated)" name="screenshotUrls" value={formData.screenshotUrls} onChange={handleChange} helperText="Enter direct image URLs, separated by commas." disabled={isSubmitting} />

                <Button variant="outlined" component="label" fullWidth sx={{ mt: 2, mb: 1 }} disabled={isSubmitting}>
                    {fileUploadText}
                    <input type="file" hidden accept=".schem" onChange={handleFileChange} disabled={isSubmitting} />
                </Button>

                <Box sx={{ display: 'flex', gap: 2, mt: 3, mb: 2 }}>
                    <Button type="submit" fullWidth variant="contained" disabled={isSubmitting} startIcon={isSubmitting ? <CircularProgress size={20} color="inherit" /> : null}>
                        {submitButtonText}
                    </Button>
                    <Button type="button" fullWidth variant="outlined" onClick={onCancelForm} disabled={isSubmitting}>
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
        authorNames: PropTypes.string,
        themes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        themeNames: PropTypes.string,
        description: PropTypes.string,
        colors: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        colorNames: PropTypes.string,
        screenshots: PropTypes.arrayOf(PropTypes.string),
        screenshotUrls: PropTypes.string,
    }),
    onBuildUpdated: PropTypes.func.isRequired,
    onCancelForm: PropTypes.func.isRequired,
};

BuildForm.defaultProps = {
    existingBuild: null,
};

export default BuildForm;